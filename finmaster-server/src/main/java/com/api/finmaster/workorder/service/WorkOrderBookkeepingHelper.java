package com.qykj.finmaster.workorder.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.FinancialConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.common.utils.third.req.AddAccountSetMigrationDataReq;
import com.qykj.finmaster.common.utils.third.req.CreateAccountSetReq;
import com.qykj.finmaster.customer.company.domain.CompanyEmployee;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CompanyEmployeeManager;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.migration.domain.FinancialDataMigration;
import com.qykj.finmaster.migration.manager.FinancialDataMigrationManager;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.taxPlaceLocation.entity.dto.GetTaxPlaceLocationUserDto;
import com.qykj.finmaster.system.taxPlaceLocation.manager.TaxPlaceLocationUserManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 代账任务单辅助：FINANCE_RECEIVE 节点后自动建账套 + 数据迁移
 * 从旧 CommonServiceImpl.createAccountSet / accountDataMigration 端口而来
 * 数据来源改为 customer_company 表（不再依赖 taskData JSON）
 */
@Slf4j
@Service
public class WorkOrderBookkeepingHelper {

    @Autowired
    private CustomerCompanyManager customerCompanyManager;

    @Autowired
    private TaxPlaceLocationUserManager taxPlaceLocationUserManager;

    @Autowired
    private FinancialDataMigrationManager financialDataMigrationManager;

    @Autowired
    private CompanyEmployeeManager companyEmployeeManager;

    @Autowired
    private com.qykj.finmaster.third.tianyancha.manager.TycCompanyInfoManager tycCompanyInfoManager;

    @Autowired
    private com.qykj.finmaster.customer.company.manager.CompanyTaxCredentialManager companyTaxCredentialManager;

    /**
     * 管家节点完成时调用：根据公司"税局所在地"解析对应的财务负责人，用于自动指派 FINANCE_RECEIVE 节点。
     * @return [username, realName]；解析失败返回 null
     */
    public String[] resolveFinanceOwnerByCompany(Long companyId) {
        if (companyId == null) return null;
        CustomerCompany cc = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        if (cc == null || StringUtils.isBlank(cc.getTaxDistrict())) {
            log.warn("[财务自动分配] 公司或税局所在地为空 companyId={}", companyId);
            return null;
        }
        GetTaxPlaceLocationUserDto dto = taxPlaceLocationUserManager.getBaseMapper()
                .getUserByProvince(cc.getTaxDistrict());
        if (dto == null || StringUtils.isBlank(dto.getUserName())) {
            log.warn("[财务自动分配] 省份 {} 未配置财务负责人", cc.getTaxDistrict());
            return null;
        }
        return new String[]{ dto.getUserName(), StringUtils.defaultString(dto.getRealName()) };
    }

    /**
     * 代账任务"财务收到"节点完成后触发，按顺序做 4 件事：
     *   1) 创建账套（taxNumber 已存在则跳过创建，进入步骤 4）
     *   2) 体验账套数据迁移（experience_account_set_id != null 时，把体验数据迁到正式账套）
     *   3) 导入人员信息同步（兼容老数据：体验账套已有的员工先合并到本地，再推送到正式账套）
     *   4) 账套信息更新（已有账套时，把 CRM 端最新公司/法人/凭证等字段差异同步到财务系统）
     * 任意一步失败不抛异常，只记录日志，避免影响任务流转
     */
    public void onFinanceReceived(Long companyId, LoginUser loginUser) {
        onFinanceReceived(companyId, null, loginUser);
    }

    /** 重载：传入 work_order 以读取 account_set_enable_date 等字段 */
    public void onFinanceReceived(Long companyId, com.qykj.finmaster.workorder.domain.WorkOrder wo, LoginUser loginUser) {
        if (companyId == null) return;
        CustomerCompany cc = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        if (cc == null) {
            log.warn("[账套创建] 未找到 companyId={} 对应的公司", companyId);
            return;
        }
        // 1) 创建账套
        try {
            createAccountSetIfAbsent(cc, wo, loginUser);
        } catch (Exception e) {
            log.error("[账套创建] 异常，companyId={}", companyId, e);
        }
        // 2) 体验账套数据迁移
        try {
            migrateAccountSetData(cc, loginUser);
        } catch (Exception e) {
            log.error("[账套数据迁移] 异常，companyId={}", companyId, e);
        }
        // 3) 导入人员信息同步（兼容体验账套老员工）
        try {
            pushEmployeesToFinancial(cc);
        } catch (Exception e) {
            log.error("[员工同步-个税申报] 异常，companyId={}", companyId, e);
        }
        // 4) 账套信息更新（与正式账套差异同步）
        try {
            updateAccountSetInfo(cc, wo, loginUser);
        } catch (Exception e) {
            log.error("[账套信息更新] 异常，companyId={}", companyId, e);
        }
    }

    /**
     * 让步补齐 → 财务系统增量同步入口。
     * 仅当财务系统侧已经存在账套时才推送：
     *   - 已有账套 → 调 updateAccountSet 把 CRM 最新字段（含本次补齐项）推给财务，再推一遍员工
     *   - 没账套（会计还没完成 FINANCE_RECEIVE）→ 跳过；后续会计完成节点时会自动用最新数据建账套，效果等价
     */
    public void syncOnConcessionResolved(Long companyId, com.qykj.finmaster.workorder.domain.WorkOrder wo, LoginUser loginUser) {
        if (companyId == null) return;
        CustomerCompany cc = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        if (cc == null || StringUtils.isBlank(cc.getTaxNumber())) {
            log.info("[让步补齐→财务系统] 公司或税号为空，跳过 companyId={}", companyId);
            return;
        }
        // 按税号查财务系统：账套存在 → 更新；不存在 → 不创建，等待下一节点（会计 FINANCE_RECEIVE）一并对接
        JSONObject exist;
        try {
            Long creatorId = resolveFinancialUserId(loginUser, cc.getTaxDistrict(), cc.getId());
            exist = FinancialUtils.getAccountSetsBeforeCreate(cc.getTaxNumber(), creatorId);
        } catch (Exception e) {
            log.error("[让步补齐→财务系统] 查账套异常 taxNumber={}", cc.getTaxNumber(), e);
            return;
        }
        if (exist == null || exist.isEmpty()) {
            log.info("[让步补齐→财务系统] 财务系统暂无账套，跳过同步；等待会计完成 FINANCE_RECEIVE 节点时统一对接 taxNumber={}",
                    cc.getTaxNumber());
            return;
        }
        // 账套已存在：把补齐后的完整数据推过去（账套字段更新 + 员工补齐推送）
        try {
            updateAccountSetInfo(cc, wo, loginUser);
        } catch (Exception e) {
            log.error("[让步补齐→财务系统] 账套更新失败 taxNumber={}", cc.getTaxNumber(), e);
        }
        try {
            pushEmployeesToFinancial(cc);
        } catch (Exception e) {
            log.error("[让步补齐→财务系统] 员工同步失败 companyId={}", cc.getId(), e);
        }
    }

    /**
     * 按"谁完成节点 = 谁建账套"原则解析财务系统的 creatorId：
     *   1) 优先按 loginUser.userId 在 tax_place_location_user 里反查（一个 CRM 用户可能配了 financial_user_id）
     *   2) 找不到 → 回退到按省份反查（兼容老配置）
     *   3) 都没有 → 返回 null + 警告
     */
    private Long resolveFinancialUserId(LoginUser loginUser, String province, Long companyId) {
        // 1) 按当前操作人反查
        if (loginUser != null && loginUser.getUserId() != null) {
            com.qykj.finmaster.system.taxPlaceLocation.entity.TaxPlaceLocationUser byUser =
                    taxPlaceLocationUserManager.lambdaQuery()
                            .eq(com.qykj.finmaster.system.taxPlaceLocation.entity.TaxPlaceLocationUser::getUserId,
                                    loginUser.getUserId())
                            .isNotNull(com.qykj.finmaster.system.taxPlaceLocation.entity.TaxPlaceLocationUser::getFinancialUserId)
                            .last("LIMIT 1")
                            .one();
            if (byUser != null && byUser.getFinancialUserId() != null) {
                log.info("[账套创建] creatorId 按操作人解析成功 userId={}, financialUserId={}",
                        loginUser.getUserId(), byUser.getFinancialUserId());
                return byUser.getFinancialUserId();
            }
        }
        // 2) 按省份反查（老逻辑兜底）
        if (StringUtils.isNotBlank(province)) {
            GetTaxPlaceLocationUserDto dto = taxPlaceLocationUserManager.getBaseMapper().getUserByProvince(province);
            if (dto != null && dto.getFinancialUserId() != null) {
                log.info("[账套创建] creatorId 按省份兜底解析 province={}, financialUserId={}",
                        province, dto.getFinancialUserId());
                return dto.getFinancialUserId();
            }
        }
        // 3) 全部失败
        log.warn("[账套创建] 无法解析 creatorId：当前操作人 {} 没配 financial_user_id，省份 {} 也没配；companyId={}",
                loginUser != null ? loginUser.getUsername() : null, province, companyId);
        return null;
    }

    /* ==================== 4) 账套信息更新 ==================== */

    /**
     * 已存在正式账套时，把 CRM 端最新的公司 / 法人 / 凭证等全部字段按税号同步到财务系统。
     * 调用 POST /OutSystem2CRM/updateAccountSet（部分更新，传什么改什么）。
     */
    private void updateAccountSetInfo(CustomerCompany cc, com.qykj.finmaster.workorder.domain.WorkOrder wo, LoginUser loginUser) {
        if (StringUtils.isBlank(cc.getTaxNumber())) return;
        Long creatorId = resolveFinancialUserId(loginUser, cc.getTaxDistrict(), cc.getId());
        JSONObject exist = FinancialUtils.getAccountSetsBeforeCreate(cc.getTaxNumber(), creatorId);
        if (exist == null || exist.isEmpty()) {
            // 没有正式账套就不需要更新（步骤 1 已经创建过了，正常走不到这里）
            return;
        }
        CreateAccountSetReq req = buildCreateAccountSetReq(cc, wo);
        if (req == null) return;
        // 把 CreateAccountSetReq 转成 Map，再补 creditCode（updateAccountSet 必填）
        // 不传 creatorId 等不可改字段，财务系统更新接口会保留这些受保护字段原值
        java.util.Map<String, Object> payload = req.toBeanMap();
        payload.put("creditCode", cc.getTaxNumber());
        payload.remove("creatorId");      // 受保护字段，不传
        payload.remove("astId");          // 模板 id 不在更新场景修改
        try {
            JSONObject resp = FinancialUtils.updateAccountSet(payload);
            JSONObject data = resp != null ? resp.getJSONObject("data") : null;
            if (data != null && Boolean.TRUE.equals(data.getBoolean("success"))) {
                log.info("[账套信息更新] 成功 taxNumber={}, accountSetId={}",
                        cc.getTaxNumber(), data.get("accountSetsId"));
            } else {
                log.error("[账套信息更新] 失败 taxNumber={}, resp={}", cc.getTaxNumber(), resp);
            }
        } catch (Exception ex) {
            log.error("[账套信息更新] 接口调用异常 taxNumber={}", cc.getTaxNumber(), ex);
        }
    }

    /* ==================== 员工名册 → 财务系统（个税申报）==================== */

    /**
     * 把 company_employee 里的员工（姓名/身份证/手机号/工资）推送到财务系统做个税申报。
     * 调用顺序：步骤 2（体验账套迁移）已完成 → 此处再推 CRM 端的员工。
     * 财务系统接口 POST /OutSystem2CRM/addEmployees 一次推全量，单条独立事务，按身份证号去重，
     * 已存在则 skipped=true，不影响其它条。
     */
    private void pushEmployeesToFinancial(CustomerCompany cc) {
        if (StringUtils.isBlank(cc.getTaxNumber())) {
            log.warn("[员工同步-个税申报] 公司缺税号 id={}，跳过", cc.getId());
            return;
        }
        List<CompanyEmployee> employees = companyEmployeeManager.lambdaQuery()
                .eq(CompanyEmployee::getCompanyId, cc.getId())
                .list();
        if (CollUtil.isEmpty(employees)) {
            log.info("[员工同步-个税申报] 公司 {} 无员工名册，跳过推送", cc.getId());
            return;
        }
        // 拼批量请求体：[{ employee: {...}, positionSalary: ... }, ...]
        java.util.List<java.util.Map<String, Object>> items = new java.util.ArrayList<>();
        int localSkipped = 0;
        for (CompanyEmployee e : employees) {
            if (StringUtils.isBlank(e.getIdCard()) || StringUtils.isBlank(e.getName())) {
                localSkipped++;
                log.warn("[员工同步-个税申报] 员工缺姓名/身份证号，本地跳过 id={}", e.getId());
                continue;
            }
            java.util.Map<String, Object> employee = new java.util.HashMap<>();
            employee.put("name", e.getName());
            employee.put("idCard", e.getIdCard());
            if (StringUtils.isNotBlank(e.getPhone())) employee.put("telPhone", e.getPhone());
            employee.put("status", "在职");
            // 出生日期：前端没填则按身份证号兜底解析
            java.time.LocalDate birthDate = e.getBirthDate();
            if (birthDate == null) birthDate = parseBirthFromIdCard(e.getIdCard());
            if (birthDate != null) employee.put("birthDate", birthDate.toString());
            // 入职日期：管家在前端录入
            if (e.getOnboardingDate() != null) {
                employee.put("onboardingDate", e.getOnboardingDate().toString());
            }

            java.util.Map<String, Object> entry = new java.util.HashMap<>();
            entry.put("employee", employee);
            // 月工资 → positionSalary（结构化字段）
            if (e.getSalary() != null) entry.put("positionSalary", e.getSalary());
            items.add(entry);
        }
        if (items.isEmpty()) {
            log.info("[员工同步-个税申报] 公司 {} 无可推送员工（全部缺字段），本地跳过 {} 人", cc.getId(), localSkipped);
            return;
        }
        try {
            JSONObject resp = FinancialUtils.addEmployees(cc.getTaxNumber(), items);
            JSONObject data = resp != null ? resp.getJSONObject("data") : null;
            if (data != null && Boolean.TRUE.equals(data.getBoolean("success"))) {
                log.info("[员工同步-个税申报] 公司 {} 推送完成 total={}, created={}, skipped={}, failed={}, 本地跳过={}",
                        cc.getId(), data.get("total"), data.get("created"),
                        data.get("skipped"), data.get("failed"), localSkipped);
            } else {
                log.error("[员工同步-个税申报] 公司 {} 批量推送失败 resp={}", cc.getId(), resp);
            }
        } catch (Exception ex) {
            log.error("[员工同步-个税申报] 批量接口调用异常 companyId={}", cc.getId(), ex);
        }
    }

    /** 从 18 位身份证号解析出生日期 yyyy-MM-dd（第 7-14 位）；解析失败返回 null */
    private java.time.LocalDate parseBirthFromIdCard(String idCard) {
        if (StringUtils.isBlank(idCard)) return null;
        try {
            String s = idCard.trim();
            if (s.length() < 14) return null;
            return java.time.LocalDate.parse(s.substring(6, 14),
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception ex) {
            return null;
        }
    }

    /* ==================== 建账套 ==================== */

    private void createAccountSetIfAbsent(CustomerCompany cc, com.qykj.finmaster.workorder.domain.WorkOrder wo, LoginUser loginUser) {
        if (StringUtils.isBlank(cc.getTaxNumber())) {
            log.warn("[账套创建] 公司缺税号 id={}", cc.getId());
            return;
        }
        Long creatorId = resolveFinancialUserId(loginUser, cc.getTaxDistrict(), cc.getId());
        // 已存在则跳过创建；但视为"税号在财务系统已可用"，同步把 is_verify_tax 置 1
        JSONObject exist = FinancialUtils.getAccountSetsBeforeCreate(cc.getTaxNumber(), creatorId);
        if (exist != null && !exist.isEmpty()) {
            log.info("[账套创建] 已存在，跳过 taxNumber={}", cc.getTaxNumber());
            markCompanyTaxVerified(cc);
            return;
        }
        // 创建场景的硬约束：省份 / 纳税人类型 必须填，否则跳过创建
        if (StringUtils.isBlank(cc.getTaxDistrict())) {
            log.warn("[账套创建] 跳过创建：未设置税局所在地 companyId={}", cc.getId());
            return;
        }
        if (!DictConstants.COMPANY_TYPE_SCALE.equals(cc.getCompanyType())
                && !DictConstants.COMPANY_TYPE_GENERAL.equals(cc.getCompanyType())) {
            log.warn("[账套创建] 跳过创建：未配置纳税人类型 companyId={}", cc.getId());
            return;
        }
        CreateAccountSetReq req = buildCreateAccountSetReq(cc, wo);
        if (req == null) return;
        // 创建账套必须有 creatorId，缺失则跳过
        if (creatorId == null) {
            log.warn("[账套创建] 跳过创建：creatorId 无法解析 companyId={}", cc.getId());
            return;
        }
        req.setCreatorId(creatorId);
        JSONObject result = FinancialUtils.createAccountSet(req);
        if (result != null && Objects.equals(result.getInteger("code"), GlobalConstant.Number.TWOHUNDRED_INT)) {
            log.info("[账套创建] 成功 taxNumber={}", cc.getTaxNumber());
            // 账套已落地财务系统 → CRM 公司表标记"税密已验证"
            markCompanyTaxVerified(cc);
        } else {
            log.error("[账套创建] 失败 taxNumber={}, resp={}", cc.getTaxNumber(), result);
        }
    }

    /**
     * 财务系统账套创建/已存在 → CRM 同步将 customer_company.is_verify_tax 置 1。
     * 已经是 1 时跳过；写库失败不阻断主流程（仅记日志）。
     */
    private void markCompanyTaxVerified(CustomerCompany cc) {
        if (cc == null || cc.getId() == null) return;
        if (cc.getIsVerifyTax() != null && cc.getIsVerifyTax() == GlobalConstant.Number.ONE_INT) {
            return;
        }
        try {
            customerCompanyManager.lambdaUpdate()
                    .set(CustomerCompany::getIsVerifyTax, GlobalConstant.Number.ONE_INT)
                    .eq(CustomerCompany::getId, cc.getId())
                    .update();
            cc.setIsVerifyTax(GlobalConstant.Number.ONE_INT);
            log.info("[税密验证] 账套就绪 → 同步设置 is_verify_tax=1 companyId={}, taxNumber={}",
                    cc.getId(), cc.getTaxNumber());
        } catch (Exception e) {
            log.error("[税密验证] 同步 is_verify_tax 失败 companyId={}", cc.getId(), e);
        }
    }

    /**
     * 构建账套请求体。注意：本方法**不**填 creatorId（创建账套场景才需要，由 createAccountSetIfAbsent 单独设置）。
     * 更新场景不需要 creatorId，调用方直接用即可。
     */
    private CreateAccountSetReq buildCreateAccountSetReq(CustomerCompany cc, com.qykj.finmaster.workorder.domain.WorkOrder wo) {
        CreateAccountSetReq req = new CreateAccountSetReq();
        req.setCompanyName(cc.getCompanyName());
        req.setCreditCode(cc.getTaxNumber());
        // 启用日期：优先取任务里管家录入的，未录入回退到当月 1 号
        LocalDate enableDate = (wo != null && wo.getAccountSetEnableDate() != null)
                ? wo.getAccountSetEnableDate()
                : LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        req.setEnableDate(enableDate);
        // 截止日期：优先取任务里管家录入的，未录入回退到启用日期 + 1 年
        LocalDate expiryDate = (wo != null && wo.getAccountSetExpiryDate() != null)
                ? wo.getAccountSetExpiryDate()
                : enableDate.plusYears(1);
        req.setEffectiveDate(expiryDate);
        // 省份 = 税局所在地 → 转拼音 key（财务系统只接受 shandong / beijing 这种 key，不接受中文）
        String province = cc.getTaxDistrict();
        String provinceKey = com.qykj.finmaster.common.utils.ProvinceKeyMapper.toKey(province);
        if (StringUtils.isNotBlank(provinceKey)) {
            req.setProvince(provinceKey);
        } else if (StringUtils.isNotBlank(province)) {
            log.warn("[账套构建] 税局所在地 {} 无法映射到拼音 key，跳过该字段 companyId={}", province, cc.getId());
        }
        // 按纳税人类型选模板
        // 纳税人类型 → 模板 / 增值税种类。更新场景缺失允许放过（让财务系统保留原值）
        if (DictConstants.COMPANY_TYPE_SCALE.equals(cc.getCompanyType())) {
            req.setAstId(FinancialConstants.TEMPLATE_ID_SMALL_SCALE);
            req.setVatType(FinancialConstants.VAT_TYPE_SMALL_SCALE);
        } else if (DictConstants.COMPANY_TYPE_GENERAL.equals(cc.getCompanyType())) {
            req.setAstId(FinancialConstants.TEMPLATE_ID_COMMONLY);
            req.setVatType(FinancialConstants.VAT_TYPE_COMMONLY);
        } else {
            log.warn("[账套构建] 未配置纳税人类型 companyId={}（更新场景会按缺失字段处理）", cc.getId());
        }
        req.setElectricityTaxPhone(FinancialConstants.ELECTRICITY_TAX_PHONE);
        req.setElectricityTaxCode(FinancialConstants.ELECTRICITY_TAX_CODE);
        req.setZeroDeclarations(Boolean.FALSE);
        req.setVoucherReviewed(Boolean.FALSE);
        // 是否个体工商户：customer_company.is_individual (0/1) → Boolean
        req.setIndividualBusiness(cc.getIsIndividual() != null && cc.getIsIndividual() == 1);
        // 个税密码：从 company_tax_credential.individual_tax_password 取，传给财务系统的 individualTaxCode
        try {
            com.qykj.finmaster.customer.company.domain.CompanyTaxCredential tc =
                    companyTaxCredentialManager.lambdaQuery()
                            .eq(com.qykj.finmaster.customer.company.domain.CompanyTaxCredential::getCompanyId, cc.getId())
                            .last("LIMIT 1")
                            .one();
            if (tc != null && StringUtils.isNotBlank(tc.getIndividualTaxPassword())) {
                req.setIndividualTaxCode(tc.getIndividualTaxPassword());
            }
        } catch (Exception e) {
            log.warn("[账套构建] 拉个税密码失败 companyId={}", cc.getId(), e);
        }
        // 经营范围 + 行业（国标 GB/T 4754 代码 + 中文名）：从天眼查表按 credit_code 反查；查不到不阻断
        try {
            com.qykj.finmaster.third.tianyancha.domain.TycCompanyInfo tyc = tycCompanyInfoManager.lambdaQuery()
                    .eq(com.qykj.finmaster.third.tianyancha.domain.TycCompanyInfo::getCreditCode, cc.getTaxNumber())
                    .last("LIMIT 1")
                    .one();
            if (tyc != null) {
                req.setBusinessScope(tyc.getBusinessScope());
                req.setIndustryName(tyc.getIndustry());
                // 企业类型（组织形式）：天眼查 companyOrgType → 同步给财务系统
                req.setCompanyOrgType(tyc.getCompanyOrgType());
                // 取最细分一级的国标代码（4 位 → 3 位 → 2 位 → 1 位）
                String code = StringUtils.firstNonBlank(
                        tyc.getCategoryCodeFourth(),
                        tyc.getCategoryCodeThird(),
                        tyc.getCategoryCodeSecond(),
                        tyc.getCategoryCodeFirst()
                );
                req.setIndustryCode(code);
            }
        } catch (Exception e) {
            log.warn("[账套创建] 拉天眼查行业 / 经营范围失败 taxNumber={}", cc.getTaxNumber(), e);
        }
        return req;
    }

    /* ==================== 数据迁移（体验账套 → 正式账套） ==================== */

    private void migrateAccountSetData(CustomerCompany cc, LoginUser loginUser) {
        if (cc.getExperienceAccountSetId() == null) return;
        // 已迁移过则跳过
        List<FinancialDataMigration> migrated = financialDataMigrationManager.lambdaQuery()
                .eq(FinancialDataMigration::getExperienceAccountSetId, cc.getExperienceAccountSetId())
                .eq(FinancialDataMigration::getMigrationStatus, GlobalConstant.Number.ONE_INT)
                .list();
        if (CollUtil.isNotEmpty(migrated)) {
            log.info("[账套数据迁移] 已迁移过 experienceAccountSetId={}", cc.getExperienceAccountSetId());
            return;
        }
        JSONArray experienceMigrationData = FinancialUtils.getExperienceMigrationData(cc.getExperienceAccountSetId());
        if (CollUtil.isEmpty(experienceMigrationData)) return;

        List<FinancialDataMigration> list = experienceMigrationData.stream().map(item -> {
            FinancialDataMigration data = CopierCopyUtils.convertTo(item, FinancialDataMigration.class);
            data.setMigrationStatus(GlobalConstant.Number.ZERO_INT);
            data.setCreateTime(new Date());
            if (loginUser != null) {
                data.setCreateBy(loginUser.getUsername());
                data.setCreateByName(loginUser.getRealName());
            }
            return data;
        }).toList();
        financialDataMigrationManager.saveBatch(list);

        Long creatorId = resolveFinancialUserId(loginUser, cc.getTaxDistrict(), cc.getId());
        JSONObject accountSet = FinancialUtils.getAccountSetsBeforeCreate(cc.getTaxNumber(), creatorId);
        if (accountSet == null || accountSet.isEmpty()) {
            log.warn("[账套数据迁移] 正式账套不存在 taxNumber={}", cc.getTaxNumber());
            return;
        }
        Long accountSetId = accountSet.getLong("id");
        list.forEach(item -> item.setAccountSetId(accountSetId));
        Boolean ok = FinancialUtils.addAccountSetMigrationData(
                CopierCopyUtils.convertTo(list, AddAccountSetMigrationDataReq.class));
        if (Boolean.TRUE.equals(ok)) {
            list.forEach(item -> {
                item.setMigrationStatus(GlobalConstant.Number.ONE_INT);
                item.setUpdateTime(new Date());
                if (loginUser != null) {
                    item.setUpdateBy(loginUser.getUsername());
                    item.setUpdateByName(loginUser.getRealName());
                }
            });
            financialDataMigrationManager.updateBatchById(list);
            log.info("[账套数据迁移] 完成 taxNumber={}", cc.getTaxNumber());
        } else {
            log.error("[账套数据迁移] 推送正式账套失败 taxNumber={}", cc.getTaxNumber());
        }
    }
}
