package com.qykj.finmaster.workorder.service;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.customer.company.domain.CompanyEmployee;
import com.qykj.finmaster.customer.company.domain.CompanyLegalPerson;
import com.qykj.finmaster.customer.company.domain.CompanyTaxCredential;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.domain.req.SaveCompanyTaxCredentialReq;
import com.qykj.finmaster.customer.company.manager.CompanyEmployeeManager;
import com.qykj.finmaster.customer.company.manager.CompanyLegalPersonManager;
import com.qykj.finmaster.customer.company.manager.CompanyTaxCredentialManager;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.common.utils.ProvinceParser;
import com.qykj.finmaster.common.utils.third.SlsjIdCardUtil;
import com.qykj.finmaster.common.utils.third.resp.SlsjIdCardResp;
import com.qykj.finmaster.customer.company.service.CompanyTaxCredentialService;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.third.idcard.service.IdCardVerifyServiceImpl;
import com.qykj.finmaster.workorder.domain.req.BookkeepingDispatchPayload;
import com.qykj.finmaster.workorder.domain.resp.BookkeepingDispatchPreloadResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代账派工辅助：预填 / 保存 4 块嵌套数据（公司+法人 / 股东 / 员工 / 税务凭证）
 * 与任务单创建分离，便于单元测试和未来复用
 */
@Slf4j
@Service
public class BookkeepingDispatchHelper {

    @Autowired
    private CustomerCompanyManager customerCompanyManager;

    @Autowired
    private CompanyLegalPersonManager legalPersonManager;

    @Autowired
    private CompanyEmployeeManager employeeManager;

    @Autowired
    private CompanyTaxCredentialManager taxCredentialManager;

    @Autowired
    private CompanyTaxCredentialService taxCredentialService;

    @Autowired
    private IdCardVerifyServiceImpl idCardVerifyService;

    /* ==================== 预填（派工弹窗打开时调用） ==================== */

    public BookkeepingDispatchPreloadResp preload(Long companyId) {
        if (companyId == null) throw new BusinessException("公司ID不能为空");
        CustomerCompany cc = customerCompanyManager.getById(companyId);
        if (cc == null) throw new BusinessException("公司不存在 id=" + companyId);

        BookkeepingDispatchPreloadResp resp = new BookkeepingDispatchPreloadResp();
        resp.setCompanyInfo(cc);
        resp.setShareholders(legalPersonManager.lambdaQuery()
                .eq(CompanyLegalPerson::getCompanyId, companyId)
                .orderByDesc(CompanyLegalPerson::getShareholdingRatio)
                .list());
        resp.setEmployees(employeeManager.lambdaQuery()
                .eq(CompanyEmployee::getCompanyId, companyId)
                .orderByAsc(CompanyEmployee::getId)
                .list());
        resp.setTaxCredential(taxCredentialManager.lambdaQuery()
                .eq(CompanyTaxCredential::getCompanyId, companyId)
                .one());
        return resp;
    }

    /* ==================== 保存（创建任务单前在事务里调用） ==================== */

    /**
     * 事务已由上游 {@link WorkOrderService#createWorkOrder} 保证
     * 这里的校验：公司/法人信息 + 股东 + 员工必填；税务凭证可选
     */
    public void saveAll(Long companyId, BookkeepingDispatchPayload payload, LoginUser loginUser, Long tenantId) {
        if (payload == null) throw new BusinessException("代账派工信息不能为空");
        if (companyId == null) throw new BusinessException("公司ID不能为空");

        validate(payload);

        // IDV-04：保存前实名核验（法人/股东/员工），非 01 一律拦截
        validateIdCards(payload);

        Date now = new Date();
        String opUser = loginUser != null ? loginUser.getUsername() : null;
        String opName = loginUser != null ? loginUser.getRealName() : null;

        // 1) 公司+法人基本信息（upsert）
        if (payload.getCompanyInfo() != null) {
            CustomerCompany incoming = payload.getCompanyInfo();
            CustomerCompany db = customerCompanyManager.getById(companyId);
            if (db == null) throw new BusinessException("公司不存在 id=" + companyId);
            copyCompanyFields(incoming, db);
            // 注册地址变了 + 前端没显式覆盖 province/taxDistrict 时自动按地址识别
            if (StringUtils.isNotBlank(incoming.getRegisteredAddress())) {
                if (incoming.getProvince() == null) {
                    String parsed = ProvinceParser.parseProvince(db.getRegisteredAddress());
                    if (StringUtils.isNotBlank(parsed)) db.setProvince(parsed);
                }
                if (incoming.getTaxDistrict() == null) {
                    String parsed = ProvinceParser.parseTaxDistrict(db.getRegisteredAddress());
                    if (StringUtils.isNotBlank(parsed)) db.setTaxDistrict(parsed);
                }
            }
            db.setUpdateBy(opUser);
            db.setUpdateByName(opName);
            db.setUpdateTime(now);
            customerCompanyManager.updateById(db);
        }

        // 2) 股东（全量替换：先软删现有，再批量插入）
        replaceShareholders(companyId, tenantId, payload.getShareholders(), opUser, opName, now);

        // 3) 员工名册（全量替换）
        replaceEmployees(companyId, tenantId, payload.getEmployees(), opUser, opName, now);

        // 4) 税务凭证（委托给 CompanyTaxCredentialService，保持与公司详情页/小程序同一套 upsert 逻辑）
        CompanyTaxCredential tc = payload.getTaxCredential();
        if (tc != null) {
            SaveCompanyTaxCredentialReq tcReq = new SaveCompanyTaxCredentialReq();
            tcReq.setCompanyId(companyId);
            tcReq.setIndividualTaxPassword(tc.getIndividualTaxPassword());
            tcReq.setIndividualTaxAppDownloaded(tc.getIndividualTaxAppDownloaded());
            tcReq.setETaxPassword(tc.getETaxPassword());
            tcReq.setETaxAppDownloaded(tc.getETaxAppDownloaded());
            tcReq.setTaxOfficerConfigured(tc.getTaxOfficerConfigured());
            tcReq.setAgentChannelConfigured(tc.getAgentChannelConfigured());
            taxCredentialService.save(tcReq, loginUser);
        }
    }

    /**
     * 任务详情页"按需保存"：不强校验、不传的块不动
     * companyInfo / shareholders / employees / taxCredential 各为 null 时跳过对应数据
     */
    public void savePartial(Long companyId, BookkeepingDispatchPayload payload, LoginUser loginUser, Long tenantId) {
        if (payload == null) throw new BusinessException("代账资料不能为空");
        if (companyId == null) throw new BusinessException("公司ID不能为空");

        // IDV-08：自动保存只存草稿、不在此核验（核验改到完成时 validateIdCardsByCompany 硬拦截）

        Date now = new Date();
        String opUser = loginUser != null ? loginUser.getUsername() : null;
        String opName = loginUser != null ? loginUser.getRealName() : null;

        if (payload.getCompanyInfo() != null) {
            CustomerCompany incoming = payload.getCompanyInfo();
            CustomerCompany db = customerCompanyManager.getById(companyId);
            if (db == null) throw new BusinessException("公司不存在 id=" + companyId);
            copyCompanyFields(incoming, db);
            if (StringUtils.isNotBlank(incoming.getRegisteredAddress())) {
                if (incoming.getProvince() == null) {
                    String parsed = ProvinceParser.parseProvince(db.getRegisteredAddress());
                    if (StringUtils.isNotBlank(parsed)) db.setProvince(parsed);
                }
                if (incoming.getTaxDistrict() == null) {
                    String parsed = ProvinceParser.parseTaxDistrict(db.getRegisteredAddress());
                    if (StringUtils.isNotBlank(parsed)) db.setTaxDistrict(parsed);
                }
            }
            db.setUpdateBy(opUser);
            db.setUpdateByName(opName);
            db.setUpdateTime(now);
            customerCompanyManager.updateById(db);
        }
        if (payload.getShareholders() != null) {
            replaceShareholders(companyId, tenantId, payload.getShareholders(), opUser, opName, now);
        }
        if (payload.getEmployees() != null) {
            replaceEmployees(companyId, tenantId, payload.getEmployees(), opUser, opName, now);
        }
        if (payload.getTaxCredential() != null) {
            CompanyTaxCredential tc = payload.getTaxCredential();
            SaveCompanyTaxCredentialReq tcReq = new SaveCompanyTaxCredentialReq();
            tcReq.setCompanyId(companyId);
            tcReq.setIndividualTaxPassword(tc.getIndividualTaxPassword());
            tcReq.setIndividualTaxAppDownloaded(tc.getIndividualTaxAppDownloaded());
            tcReq.setETaxPassword(tc.getETaxPassword());
            tcReq.setETaxAppDownloaded(tc.getETaxAppDownloaded());
            tcReq.setTaxOfficerConfigured(tc.getTaxOfficerConfigured());
            tcReq.setAgentChannelConfigured(tc.getAgentChannelConfigured());
            taxCredentialService.save(tcReq, loginUser);
        }
    }

    /* ==================== 校验（必填，税务凭证除外） ==================== */

    private void validate(BookkeepingDispatchPayload p) {
        CustomerCompany ci = p.getCompanyInfo();
        if (ci == null) throw new BusinessException("公司信息必填");
        requireText(ci.getCompanyName(), "公司名称");
        requireText(ci.getTaxNumber(), "公司税号");
        requireText(ci.getCompanyType(), "纳税人类型");
        requireText(ci.getLegalName(), "法人姓名");
        requireText(ci.getLegalIdCard(), "法人身份证号");
        requireText(ci.getLegalPhone(), "法人手机号");
        requireText(ci.getRegisteredAddress(), "注册地址");
        requireText(ci.getProvince(), "省份");
        requireText(ci.getTaxDistrict(), "税局所在地");
        if (ci.getEstablishedDate() == null) throw new BusinessException("成立日期必填");

        // 股东列表 0 条放过；填了任何一行只校验姓名 + 持股比例（身份证号可选）
        if (CollUtil.isNotEmpty(p.getShareholders())) {
            for (CompanyLegalPerson s : p.getShareholders()) {
                requireText(s.getName(), "股东姓名");
                if (s.getShareholdingRatio() == null
                        || s.getShareholdingRatio().compareTo(BigDecimal.ZERO) < 0) {
                    throw new BusinessException("股东「" + s.getName() + "」的持股比例不合法");
                }
            }
        }

        if (CollUtil.isEmpty(p.getEmployees())) {
            throw new BusinessException("员工名册至少录入 1 条");
        }
        for (CompanyEmployee e : p.getEmployees()) {
            requireText(e.getName(), "员工姓名");
            requireText(e.getIdCard(), "员工身份证号");
        }
        // 税务凭证可选，不做必填校验
    }

    private void requireText(String v, String label) {
        if (StringUtils.isBlank(v)) throw new BusinessException(label + "必填");
    }

    /* ==================== 实名核验（IDV-04：保存即核验，全部拦截非 01） ==================== */

    /**
     * 收集本次 payload 的 (姓名, 身份证号)：法人 + 股东(idCard 非空) + 员工(idCard 非空)，按 (name,idCard) 去重，
     * 逐一调实名核验（缓存优先，IDV-03）。任一非 01 即汇总拦截、抛 BusinessException。身份证日志脱敏；多人串行。
     */
    private void validateIdCards(BookkeepingDispatchPayload payload) {
        // key=nameidCard → [role, name, idCard]，保序、去重（法人与股东可能同人）
        LinkedHashMap<String, String[]> targets = new LinkedHashMap<>();
        CustomerCompany ci = payload.getCompanyInfo();
        // IDV-05：境外证件（region=1）完全不调 verify、不做 18 位预校验、不拦截，只照常存
        if (ci != null && StringUtils.isNotBlank(ci.getLegalIdCard()) && !isNonIdCard(ci.getLegalIdType())) {
            addTarget(targets, "法人", ci.getLegalName(), ci.getLegalIdCard());
        }
        if (CollUtil.isNotEmpty(payload.getShareholders())) {
            for (CompanyLegalPerson s : payload.getShareholders()) {
                if (StringUtils.isNotBlank(s.getIdCard()) && !isNonIdCard(s.getIdType())) {
                    addTarget(targets, "股东", s.getName(), s.getIdCard());
                }
            }
        }
        if (CollUtil.isNotEmpty(payload.getEmployees())) {
            for (CompanyEmployee e : payload.getEmployees()) {
                if (StringUtils.isNotBlank(e.getIdCard()) && !isNonIdCard(e.getIdType())) addTarget(targets, "员工", e.getName(), e.getIdCard());
            }
        }
        if (targets.isEmpty()) return;

        List<String> failures = new ArrayList<>();
        for (String[] t : targets.values()) {
            String role = t[0], name = t[1], idCard = t[2];
            SlsjIdCardResp resp = idCardVerifyService.verify(name, idCard);
            String rc = resp == null ? null : resp.getResultCode();
            if ("01".equals(rc)) continue; // 一致放行
            String reason;
            if ("02".equals(rc)) {
                reason = "姓名与身份证号不一致，请核对";
            } else if ("03".equals(rc)) {
                reason = "核验不确定，请核实后重试";
            } else { // 04 / code!=0 / 预校验不过
                String msg = (resp != null && StringUtils.isNotBlank(resp.getMsg())) ? resp.getMsg() : "未知错误";
                reason = "核验服务暂不可用（" + msg + "），请稍后重试或联系管理员";
            }
            failures.add("『" + role + "·" + name + "』" + reason);
            log.warn("[idcard-verify] 代账保存核验未通过 role={} name={} idCard={} resultCode={} msg={}",
                    role, name, SlsjIdCardUtil.mask(idCard), rc, resp == null ? null : resp.getMsg());
        }
        if (!failures.isEmpty()) {
            throw new BusinessException(String.join("；", failures));
        }
    }

    /**
     * 按公司从库里取 法人/股东/员工 组装 payload 后实名核验（IDV-08：完成时硬核验拦截）。
     * 复用 {@link #validateIdCards}（境外跳过、缓存优先、不一致抛 BusinessException）。
     * 在 WorkOrderService.executeCurrentStep 的代账 HANDYMAN_FILL 块调用——不一致抛异常，外层事务整体回滚、工单留 RUNNING。
     */
    public void validateIdCardsByCompany(Long companyId) {
        if (companyId == null) return;
        CustomerCompany company = customerCompanyManager.getById(companyId);
        BookkeepingDispatchPayload p = new BookkeepingDispatchPayload();
        p.setCompanyInfo(company);
        p.setShareholders(legalPersonManager.lambdaQuery()
                .eq(CompanyLegalPerson::getCompanyId, companyId)
                .eq(CompanyLegalPerson::getDeleted, 0)
                .list());
        p.setEmployees(employeeManager.lambdaQuery()
                .eq(CompanyEmployee::getCompanyId, companyId)
                .eq(CompanyEmployee::getDeleted, 0)
                .list());
        validateIdCards(p);
    }

    /** 非身份证证件（type=1，如港澳台/护照等）：免身份证二要素核验。 */
    private boolean isNonIdCard(Integer t) {
        return t != null && t == 1;
    }

    private void addTarget(LinkedHashMap<String, String[]> map, String role, String name, String idCard) {
        String key = (name == null ? "" : name.trim()) + "" + (idCard == null ? "" : idCard.trim());
        map.putIfAbsent(key, new String[]{role, name, idCard});
    }

    /* ==================== 字段拷贝（只覆盖前端允许编辑的几列，避免覆盖 openid/tenant_id 等） ==================== */

    private void copyCompanyFields(CustomerCompany src, CustomerCompany dst) {
        if (src.getCompanyName() != null) dst.setCompanyName(src.getCompanyName());
        if (src.getTaxNumber() != null) dst.setTaxNumber(src.getTaxNumber());
        if (src.getCompanyType() != null) dst.setCompanyType(src.getCompanyType());
        if (src.getLegalName() != null) dst.setLegalName(src.getLegalName());
        if (src.getLegalIdCard() != null) dst.setLegalIdCard(src.getLegalIdCard());
        if (src.getLegalIdType() != null) dst.setLegalIdType(src.getLegalIdType()); // IDV-07：法人证件类型持久化
        if (src.getLegalPhone() != null) dst.setLegalPhone(src.getLegalPhone());
        if (src.getEstablishedDate() != null) dst.setEstablishedDate(src.getEstablishedDate());
        if (src.getRegStatus() != null) dst.setRegStatus(src.getRegStatus());
        if (src.getRegisteredAddress() != null) dst.setRegisteredAddress(src.getRegisteredAddress());
        if (src.getTaxDistrict() != null) dst.setTaxDistrict(src.getTaxDistrict());
        if (src.getProvince() != null) dst.setProvince(src.getProvince());
        if (src.getIsInvoicing() != null) dst.setIsInvoicing(src.getIsInvoicing());
        if (src.getIsIndividual() != null) dst.setIsIndividual(src.getIsIndividual());
        if (src.getCompanyStatus() != null) dst.setCompanyStatus(src.getCompanyStatus());
    }

    /* ==================== 股东全量替换 ==================== */

    private void replaceShareholders(Long companyId, Long tenantId, List<CompanyLegalPerson> incoming,
                                     String opUser, String opName, Date now) {
        if (incoming == null) incoming = Collections.emptyList();
        // 软删现有
        legalPersonManager.lambdaUpdate()
                .eq(CompanyLegalPerson::getCompanyId, companyId)
                .set(CompanyLegalPerson::getDeleted, 1)
                .set(CompanyLegalPerson::getUpdateBy, opUser)
                .set(CompanyLegalPerson::getUpdateByName, opName)
                .set(CompanyLegalPerson::getUpdateTime, now)
                .update();
        if (incoming.isEmpty()) return;
        for (CompanyLegalPerson s : incoming) {
            s.setId(null);
            s.setCompanyId(companyId);
            s.setDeleted(0);
            s.setCreateBy(opUser);
            s.setCreateByName(opName);
            s.setCreateTime(now);
            s.setUpdateTime(now);
            legalPersonManager.save(s);
        }
    }

    /* ==================== 员工合并保存（按身份证号去重） ==================== */

    /**
     * 员工名册保存策略：
     * 1. 身份证号已存在于该公司 → 跳过，不重复录入
     * 2. 身份证号不存在 → 插入新记录
     * 3. 原有员工不在本次传入列表 → 软删（视为从名册移除）
     */
    private void replaceEmployees(Long companyId, Long tenantId, List<CompanyEmployee> incoming,
                                  String opUser, String opName, Date now) {
        if (incoming == null) incoming = Collections.emptyList();

        // 查询该公司当前活跃员工（@TableLogic 自动过滤 deleted=1）
        List<CompanyEmployee> existing = employeeManager.lambdaQuery()
                .eq(CompanyEmployee::getCompanyId, companyId)
                .list();

        // 按身份证号建索引，用于去重
        Map<String, CompanyEmployee> existingByIdCard = existing.stream()
                .filter(e -> StringUtils.isNotBlank(e.getIdCard()))
                .collect(Collectors.toMap(CompanyEmployee::getIdCard, e -> e, (a, b) -> a));

        // 本次传入的身份证号集合，用于判断哪些旧员工需要软删
        Set<String> incomingIdCards = incoming.stream()
                .map(CompanyEmployee::getIdCard)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        // 软删：在 DB 中存在但本次未传入的员工（视为从名册移除）
        for (CompanyEmployee ex : existing) {
            if (StringUtils.isBlank(ex.getIdCard()) || !incomingIdCards.contains(ex.getIdCard())) {
                employeeManager.lambdaUpdate()
                        .eq(CompanyEmployee::getId, ex.getId())
                        .set(CompanyEmployee::getDeleted, 1)
                        .set(CompanyEmployee::getUpdateBy, opUser)
                        .set(CompanyEmployee::getUpdateByName, opName)
                        .set(CompanyEmployee::getUpdateTime, now)
                        .update();
            }
        }

        // INSERT 新身份证号 / UPDATE 已存在身份证号（之前是"已存在就跳过"，导致后续字段编辑丢失）
        for (CompanyEmployee e : incoming) {
            // 出生日期按身份证号自动解析（前端没传时由后端兜底；前端已传则尊重）
            if (e.getBirthDate() == null && StringUtils.isNotBlank(e.getIdCard())) {
                e.setBirthDate(parseBirthFromIdCard(e.getIdCard()));
            }

            CompanyEmployee existed = StringUtils.isNotBlank(e.getIdCard())
                    ? existingByIdCard.get(e.getIdCard())
                    : null;
            if (existed != null) {
                // UPDATE：保留主键 / tenantId / companyId / createXxx，更新可变字段
                CompanyEmployee upd = new CompanyEmployee();
                upd.setId(existed.getId());
                upd.setName(e.getName());
                upd.setIdCard(e.getIdCard());
                upd.setPhone(e.getPhone());
                upd.setSalary(e.getSalary());
                upd.setBirthDate(e.getBirthDate());
                upd.setOnboardingDate(e.getOnboardingDate());
                upd.setUpdateBy(opUser);
                upd.setUpdateByName(opName);
                upd.setUpdateTime(now);
                employeeManager.updateById(upd);
            } else {
                // INSERT
                e.setId(null);
                e.setTenantId(tenantId);
                e.setCompanyId(companyId);
                e.setDeleted(0);
                e.setCreateBy(opUser);
                e.setCreateByName(opName);
                e.setCreateTime(now);
                e.setUpdateTime(now);
                employeeManager.save(e);
            }
        }
    }

    /** 从 18 位身份证号解析出生日期 yyyy-MM-dd（第 7-14 位）；解析失败返回 null */
    private java.time.LocalDate parseBirthFromIdCard(String idCard) {
        try {
            String s = idCard.trim();
            if (s.length() < 14) return null;
            String yyyymmdd = s.substring(6, 14);
            return java.time.LocalDate.parse(yyyymmdd,
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception ex) {
            return null;
        }
    }
}
