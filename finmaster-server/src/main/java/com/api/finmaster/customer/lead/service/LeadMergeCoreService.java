package com.qykj.finmaster.customer.lead.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.customer.company.domain.CompanyContact;
import com.qykj.finmaster.customer.company.domain.CompanyEmployee;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CompanyContactManager;
import com.qykj.finmaster.customer.company.manager.CompanyEmployeeManager;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.customer.company.service.CustomerCompanyBroadcaster;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.CustomerInfoManager;
import com.qykj.finmaster.customer.customerInfoLog.domain.CustomerLog;
import com.qykj.finmaster.customer.customerInfoLog.manager.CustomerLogManager;
import com.qykj.finmaster.customer.customerInfoLog.service.CustomerLogServiceImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.entity.LeadMergeHistory;
import com.qykj.finmaster.customer.lead.entity.LeadPhone;
import com.qykj.finmaster.customer.lead.entity.req.AddLeadPhoneReq;
import com.qykj.finmaster.customer.lead.entity.resp.LeadPhoneResp;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.customer.lead.manager.LeadManager;
import com.qykj.finmaster.customer.lead.manager.LeadMergeHistoryManager;
import com.qykj.finmaster.customer.lead.manager.LeadPhoneManager;
import com.qykj.finmaster.customer.lead.mapper.LeadPhoneMapper;
import com.qykj.finmaster.customer.leadLog.entity.LeadLog;
import com.qykj.finmaster.customer.leadLog.manager.LeadLogManager;
import com.qykj.finmaster.customer.leadLog.service.LeadLogServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 线索合并核心服务。
 *
 * 支持入口：
 *   1. PC "加手机号"动作（{@link #addPhone}）
 *   2. 旧 mergeLead 重写（{@link #mergeByOldApi}）
 *
 * 合并语义：
 *   - 不合并 customer_info，让多个 customer 共挂同一个 target lead（lead_id 重指）
 *   - customer_company 同税号并存，靠 broadcaster 保持关键字段一致
 *   - sourceLead 软删，phone/openId/wechat 进 lead_phone 子表
 *   - 跨租户：target lead 之 tenant_id 视为权威，所有家族成员搬迁
 *
 * 6 个分支差异由本服务内部根据 sourceLead 的客户/公司情况自动判定。
 */
@Service
public class LeadMergeCoreService {

    private static final Logger log = LoggerFactory.getLogger(LeadMergeCoreService.class);

    /** 兼容旧引用：转发到 DictConstants */
    public static final int LOG_TYPE_MERGE = DictConstants.LEAD_LOG_TYPE_MERGE;
    public static final int CUSTOMER_LOG_TYPE_MERGE = DictConstants.CUSTOMER_LOG_TYPE_MERGE_REBIND;

    @Autowired private LeadManager leadManager;
    @Autowired private LeadPhoneManager leadPhoneManager;
    @Autowired private LeadPhoneMapper leadPhoneMapper;
    @Autowired private LeadLogManager leadLogManager;
    @Autowired private LeadLogServiceImpl leadLogService;
    @Autowired private LeadMergeHistoryManager leadMergeHistoryManager;
    @Autowired private CustomerInfoManager customerInfoManager;
    @Autowired private CustomerCompanyManager customerCompanyManager;
    @Autowired private CompanyContactManager companyContactManager;
    @Autowired private CompanyEmployeeManager companyEmployeeManager;
    @Autowired private CustomerLogServiceImpl customerLogService;
    @Autowired private CustomerLogManager customerLogManager;
    @Autowired private CustomerCompanyBroadcaster customerCompanyBroadcaster;

    // ============================================================
    // 公共入口
    // ============================================================

    /**
     * PC "加手机号"入口：把新的通信号添加到 target lead；
     * 若新号已在系统中作为其它 sourceLead 的通信号存在 → 触发合并。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addPhone(AddLeadPhoneReq req) {
        if (req == null || req.getLeadId() == null) {
            throw new LeadMergeException("线索ID不能为空");
        }
        if (StringUtils.isBlank(req.getPhone())
                && StringUtils.isBlank(req.getOpenId())
                && StringUtils.isBlank(req.getWechat())) {
            throw new LeadMergeException("手机号 / 微信号 / openId 至少需填写一项");
        }

        LoginUser loginUser = getLoginUser();
        Lead target = mustGetLead(req.getLeadId());
        ensureCurrentUserCanOperate(target, loginUser);

        // 通过新手机号 / openId / wechat 查找撞库的 sourceLead
        List<Long> hitLeadIds = leadPhoneMapper.selectLeadIdsByContact(
                req.getPhone(), req.getOpenId(), req.getWechat(), target.getTenantId());
        if (hitLeadIds == null) hitLeadIds = Collections.emptyList();
        List<Long> sourceLeadIds = hitLeadIds.stream()
                .filter(id -> !Objects.equals(id, target.getId()))
                .distinct()
                .toList();

        // 1) 校验源 lead 与其客户的归属：源线索负责人必须为空或与本线索负责人一致
        if (!sourceLeadIds.isEmpty()) {
            ensureAllSourcesCanBeMerged(target, sourceLeadIds, loginUser, describeReqContact(req));
        }

        // 2) 把新通信号写入 target 子表（去重）
        upsertLeadPhone(target, req.getPhone(), req.getOpenId(), req.getWechat(),
                "pc_manual", null, req.getRemark(), loginUser);

        // 3) 对每个 sourceLead 执行合并
        for (Long sourceLeadId : sourceLeadIds) {
            mergeOne(target, sourceLeadId, loginUser);
        }

        // 4) 合并尾声：把 target 家族里所有 CC 的可广播字段在家族内同步一次
        broadcastFamilyAfterMerge(target);

        return Boolean.TRUE;
    }

    /**
     * 查询线索的所有通道行（含主表当前主通道；按 is_primary DESC + create_time ASC 排序）。
     * 主表 phone/openId/wechat 与子表 is_primary=1 的行同步；若子表里没有 is_primary=1，则补一条
     * 描述主表数据的虚拟行（id=0）作为兜底显示。
     */
    public List<LeadPhoneResp> listPhones(Long leadId) {
        Lead lead = mustGetLead(leadId);
        List<LeadPhone> rows = leadPhoneManager.lambdaQuery()
                .eq(LeadPhone::getLeadId, leadId)
                .orderByDesc(LeadPhone::getIsPrimary)
                .orderByAsc(LeadPhone::getCreateTime)
                .list();

        boolean hasPrimary = rows.stream()
                .anyMatch(r -> r.getIsPrimary() != null && r.getIsPrimary() == GlobalConstant.Number.ONE_INT);
        List<LeadPhoneResp> resp = new ArrayList<>(CopierCopyUtils.convertTo(rows, LeadPhoneResp.class));

        // 主表存在通信号但子表无主通道 → 兜底虚拟行
        if (!hasPrimary
                && (StringUtils.isNotBlank(lead.getPhone())
                || StringUtils.isNotBlank(lead.getOpenId())
                || StringUtils.isNotBlank(lead.getWechat()))) {
            LeadPhoneResp virtual = new LeadPhoneResp();
            virtual.setId(0L);
            virtual.setLeadId(leadId);
            virtual.setPhone(lead.getPhone());
            virtual.setOpenId(lead.getOpenId());
            virtual.setWechat(lead.getWechat());
            virtual.setIsPrimary(GlobalConstant.Number.ONE_INT);
            virtual.setSource("lead_main_fallback");
            virtual.setCreateTime(lead.getCreateTime());
            resp.add(0, virtual);
        }
        return resp;
    }

    /**
     * 编辑通道行（弹窗内编辑）。
     *  - 校验：新 phone / openId / wechat 不能撞到其它 lead 的通道（避免越权占用）
     *  - 更新行字段（phone / wechat / openId / remark）
     *  - 若该行 is_primary=1，同步 sale_lead 主表的 phone / open_id / wechat
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updatePhone(Long leadId, Long phoneRowId, AddLeadPhoneReq req) {
        Lead target = mustGetLead(leadId);
        LoginUser loginUser = getLoginUser();
        ensureCurrentUserCanOperate(target, loginUser);

        LeadPhone row = leadPhoneManager.getById(phoneRowId);
        if (row == null || !Objects.equals(row.getLeadId(), leadId)) {
            throw new LeadMergeException("通道行不存在或不属于该线索");
        }

        // 校验：三项至少一项非空
        if (StringUtils.isBlank(req.getPhone())
                && StringUtils.isBlank(req.getOpenId())
                && StringUtils.isBlank(req.getWechat())) {
            throw new LeadMergeException("手机号 / 微信号 / openId 至少需保留一项");
        }
        // 校验：行已绑定 openId 时，phone 与 openId 都是来自微信小程序、深度绑定的不可变对。
        // 仅允许改 wechat / 备注；任何对 phone 或 openId 的修改 / 清空都拒绝。
        if (StringUtils.isNotBlank(row.getOpenId())) {
            boolean phoneChanged  = !Objects.equals(orNull(row.getPhone()),  orNull(req.getPhone()));
            boolean openIdChanged = !Objects.equals(orNull(row.getOpenId()), orNull(req.getOpenId()));
            if (phoneChanged) {
                throw new LeadMergeException("该通道来自微信小程序，手机号与 openId 深度绑定，不可修改手机号");
            }
            if (openIdChanged) {
                throw new LeadMergeException("openId 来源于微信小程序，不可修改或清除");
            }
        }
        // 校验：新值不能撞到其它 lead 的通道
        ensureNotCollideWithOtherLeads(target, leadId, req);

        // 1) 更新子表行
        LeadPhone update = new LeadPhone();
        update.setId(phoneRowId);
        update.setPhone(StringUtils.isBlank(req.getPhone()) ? null : req.getPhone());
        update.setWechat(StringUtils.isBlank(req.getWechat()) ? null : req.getWechat());
        update.setOpenId(StringUtils.isBlank(req.getOpenId()) ? null : req.getOpenId());
        if (req.getRemark() != null) update.setRemark(req.getRemark());
        update.setUpdateBy(loginUser.getUsername());
        update.setUpdateByName(loginUser.getRealName());
        update.setUpdateTime(new Date());
        leadPhoneManager.updateById(update);

        // 2) 若是主通道，同步 sale_lead 主表
        if (row.getIsPrimary() != null && row.getIsPrimary() == GlobalConstant.Number.ONE_INT) {
            leadManager.lambdaUpdate()
                    .set(Lead::getPhone, update.getPhone())
                    .set(Lead::getWechat, update.getWechat())
                    .set(Lead::getOpenId, update.getOpenId())
                    .set(Lead::getUpdateBy, loginUser.getUsername())
                    .set(Lead::getUpdateByName, loginUser.getRealName())
                    .set(Lead::getUpdateTime, new Date())
                    .eq(Lead::getId, leadId)
                    .update();
            leadLogService.addLeadLogByCommon(leadId, DictConstants.LEAD_LOG_TYPE_FOLLOW,
                    String.format("主通道修改：phone=%s wechat=%s openId=%s",
                            nvl(update.getPhone()), nvl(update.getWechat()), nvl(update.getOpenId())));
        }
        return Boolean.TRUE;
    }

    /** 校验：req 中 phone/openId/wechat 不能撞到当前 lead 之外的其它 lead 的通道 */
    private void ensureNotCollideWithOtherLeads(Lead target, Long leadId, AddLeadPhoneReq req) {
        if (StringUtils.isNotBlank(req.getPhone())) {
            List<Long> hits = leadPhoneMapper.selectLeadIdsByContact(req.getPhone(), null, null, target.getTenantId());
            if (hits != null && hits.stream().anyMatch(id -> !Objects.equals(id, leadId))) {
                throw new LeadMergeException("手机号已存在于其它线索，无法直接修改；请通过【新增】触发合并");
            }
        }
        if (StringUtils.isNotBlank(req.getOpenId())) {
            List<Long> hits = leadPhoneMapper.selectLeadIdsByContact(null, req.getOpenId(), null, target.getTenantId());
            if (hits != null && hits.stream().anyMatch(id -> !Objects.equals(id, leadId))) {
                throw new LeadMergeException("openId 已存在于其它线索，无法直接修改；请通过【新增】触发合并");
            }
        }
        if (StringUtils.isNotBlank(req.getWechat())) {
            List<Long> hits = leadPhoneMapper.selectLeadIdsByContact(null, null, req.getWechat(), target.getTenantId());
            if (hits != null && hits.stream().anyMatch(id -> !Objects.equals(id, leadId))) {
                throw new LeadMergeException("微信号已存在于其它线索，无法直接修改；请通过【新增】触发合并");
            }
        }
    }

    /**
     * 线索编辑（updateLead）后，处理"主通道刷新"：
     *  - 若新值与原主通道完全一致 → 不动
     *  - 否则：demote 旧主通道（保留为历史），新建/复用匹配新值的子表行，置为主通道
     *
     * 由 LeadServiceImpl.updateLead 在主表 update 之后调用。
     */
    @Transactional(rollbackFor = Exception.class)
    public void afterUpdateLeadRefreshPrimary(Long leadId, String oldPhone, String oldOpenId, String oldWechat,
                                               String newPhone, String newOpenId, String newWechat,
                                               LoginUser loginUser) {
        boolean changed = !java.util.Objects.equals(orNull(oldPhone), orNull(newPhone))
                || !java.util.Objects.equals(orNull(oldOpenId), orNull(newOpenId))
                || !java.util.Objects.equals(orNull(oldWechat), orNull(newWechat));
        if (!changed) return;

        // 1) demote 当前所有 primary
        leadPhoneManager.lambdaUpdate()
                .set(LeadPhone::getIsPrimary, GlobalConstant.Number.ZERO_INT)
                .set(LeadPhone::getUpdateTime, new Date())
                .eq(LeadPhone::getLeadId, leadId)
                .eq(LeadPhone::getIsPrimary, GlobalConstant.Number.ONE_INT)
                .update();

        // 2) 查 (leadId, 新 phone, 新 openId, 新 wechat) 是否已有匹配行；有则复用，无则新建
        LeadPhone existing = leadPhoneManager.lambdaQuery()
                .eq(LeadPhone::getLeadId, leadId)
                .eq(LeadPhone::getDeleted, GlobalConstant.Number.ZERO_INT)
                .eq(newPhone != null,  LeadPhone::getPhone, newPhone)
                .isNull(newPhone == null, LeadPhone::getPhone)
                .eq(newOpenId != null, LeadPhone::getOpenId, newOpenId)
                .isNull(newOpenId == null, LeadPhone::getOpenId)
                .eq(newWechat != null, LeadPhone::getWechat, newWechat)
                .isNull(newWechat == null, LeadPhone::getWechat)
                .last("LIMIT 1")
                .one();
        if (existing != null) {
            leadPhoneManager.lambdaUpdate()
                    .set(LeadPhone::getIsPrimary, GlobalConstant.Number.ONE_INT)
                    .set(LeadPhone::getUpdateTime, new Date())
                    .eq(LeadPhone::getId, existing.getId())
                    .update();
        } else {
            Lead target = leadManager.getById(leadId);
            LeadPhone row = new LeadPhone();
            row.setTenantId(target == null ? null : target.getTenantId());
            row.setLeadId(leadId);
            row.setPhone(StringUtils.isBlank(newPhone) ? null : newPhone);
            row.setOpenId(StringUtils.isBlank(newOpenId) ? null : newOpenId);
            row.setWechat(StringUtils.isBlank(newWechat) ? null : newWechat);
            row.setIsPrimary(GlobalConstant.Number.ONE_INT);
            row.setSource("lead_update");
            if (loginUser != null) {
                row.setCreateBy(loginUser.getUsername());
                row.setCreateByName(loginUser.getRealName());
            }
            row.setCreateTime(new Date());
            leadPhoneManager.save(row);
        }
    }

    private static String orNull(String s) { return StringUtils.isBlank(s) ? null : s; }

    /**
     * 把指定子表行设为主通道：
     *  1) 该行 is_primary = 1，其它行 is_primary = 0
     *  2) 同步 sale_lead 主表的 phone / open_id / wechat 与该行对齐
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean setPrimaryPhone(Long leadId, Long phoneRowId) {
        Lead target = mustGetLead(leadId);
        LoginUser loginUser = getLoginUser();
        ensureCurrentUserCanOperate(target, loginUser);

        LeadPhone row = leadPhoneManager.getById(phoneRowId);
        if (row == null || !Objects.equals(row.getLeadId(), leadId)) {
            throw new LeadMergeException("通道行不存在或不属于该线索");
        }

        // 1) 其它行清主
        leadPhoneManager.lambdaUpdate()
                .set(LeadPhone::getIsPrimary, GlobalConstant.Number.ZERO_INT)
                .set(LeadPhone::getUpdateTime, new Date())
                .eq(LeadPhone::getLeadId, leadId)
                .ne(LeadPhone::getId, phoneRowId)
                .update();
        // 2) 当前行设主
        leadPhoneManager.lambdaUpdate()
                .set(LeadPhone::getIsPrimary, GlobalConstant.Number.ONE_INT)
                .set(LeadPhone::getUpdateTime, new Date())
                .eq(LeadPhone::getId, phoneRowId)
                .update();
        // 3) 同步主表
        leadManager.lambdaUpdate()
                .set(Lead::getPhone, row.getPhone())
                .set(Lead::getOpenId, row.getOpenId())
                .set(Lead::getWechat, row.getWechat())
                .set(Lead::getUpdateBy, loginUser.getUsername())
                .set(Lead::getUpdateByName, loginUser.getRealName())
                .set(Lead::getUpdateTime, new Date())
                .eq(Lead::getId, leadId)
                .update();
        // 4) 写一条 lead_log 留痕
        leadLogService.addLeadLogByCommon(leadId, GlobalConstant.Number.TWO_INT,
                String.format("切换主通道：phone=%s wechat=%s openId=%s",
                        nvl(row.getPhone()), nvl(row.getWechat()), nvl(row.getOpenId())));
        return Boolean.TRUE;
    }

    /**
     * 旧 mergeLead API 兼容入口（销售在 PC 撞库时用的字段选择合并）：
     *  - mergeResult 中字段覆盖 target lead 主表
     *  - 同时调用新合并核心：source 的 phone/openId/wechat 落 target 子表 + 处理客户 + 处理 CC
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean mergeByOldApi(Long sourceLeadId, Long targetLeadId, Map<String, Object> mergeResult, Long tenantId) {
        if (sourceLeadId == null || targetLeadId == null) {
            throw new LeadMergeException("源 / 目标线索ID不能为空");
        }
        LoginUser loginUser = getLoginUser();
        Lead target = mustGetLead(targetLeadId);
        ensureCurrentUserCanOperate(target, loginUser);
        ensureAllSourcesCanBeMerged(target, List.of(sourceLeadId), loginUser, null);

        // 1) 兼容旧行为：先按 mergeResult 覆盖 target 字段
        if (mergeResult != null && !mergeResult.isEmpty()) {
            updateTargetByMergeResult(targetLeadId, mergeResult, tenantId);
            target = leadManager.getById(targetLeadId);
        }

        // 2) 走新合并核心
        mergeOne(target, sourceLeadId, loginUser);
        broadcastFamilyAfterMerge(target);
        return Boolean.TRUE;
    }

    // ============================================================
    // 单条合并：target ⇐ source
    // ============================================================

    private void mergeOne(Lead target, Long sourceLeadId, LoginUser loginUser) {
        Lead source = leadManager.getById(sourceLeadId);
        if (source == null || GlobalConstant.Number.ONE_INT == orZero(source.getDeleted())) {
            // 已不存在或已软删 → 跳过
            return;
        }
        // 1) 取出 source 关联的所有客户
        List<CustomerInfo> sourceCustomers = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getLeadId, source.getId())
                .list();

        // 2) 取出这些客户名下的 CC，便于快照
        List<Long> sourceCustomerIds = sourceCustomers.stream().map(CustomerInfo::getId).toList();
        List<CustomerCompany> sourceCC = CollUtil.isEmpty(sourceCustomerIds)
                ? Collections.emptyList()
                : customerCompanyManager.lambdaQuery()
                    .in(CustomerCompany::getCustomerId, sourceCustomerIds)
                    .list();

        // 3) 写合并历史快照
        writeMergeHistory(target, source, sourceCustomers, sourceCC, loginUser);

        // 4) 把 source 主表 phone/openId/wechat 与 source 子表行都搬到 target 子表
        migratePhoneRowsToTarget(target, source, loginUser);

        // 5) 把 source 的 lead_log 全量改挂到 target
        moveLeadLogs(source.getId(), target.getId(), target.getTenantId());

        // 6) 客户层：6 分支差异在此体现
        if (CollUtil.isNotEmpty(sourceCustomers)) {
            // 分支 1.x / 2.x：source 有客户
            Long defaultCustomerId = chooseDefaultCustomerId(target, sourceCustomers);
            for (CustomerInfo cs : sourceCustomers) {
                rebindCustomerToTarget(cs, target, loginUser);
                migrateCustomerTenant(cs, target.getTenantId());
            }
            // 公司层：搬租户；不动 customer_id 与 openid
            for (CustomerCompany cc : sourceCC) {
                migrateCCTenant(cc, target.getTenantId());
            }
            // 把 source 在 target 子表新增的行（migrated_phone_rows）回填 default_customer_id
            backfillDefaultCustomerOnNewlyMigratedRows(target, source, defaultCustomerId);
        } else {
            // 分支 3：source 无客户 → 子表行 default_customer_id = target 自身已转化的某客户（若有）
            Long defaultCustomerId = chooseDefaultCustomerForOrphanSource(target);
            backfillDefaultCustomerOnNewlyMigratedRows(target, source, defaultCustomerId);
        }

        // 7) 写 target 的合并日志
        leadLogService.addLeadLogByCommon(target.getId(), LOG_TYPE_MERGE,
                String.format("合并自线索 #%d（phone=%s, openId=%s, wechat=%s）",
                        source.getId(),
                        nvl(source.getPhone()), nvl(source.getOpenId()), nvl(source.getWechat())));

        // 8) 软删 source
        leadManager.lambdaUpdate()
                .set(Lead::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(Lead::getRemark, mergedRemark(source.getRemark(), target.getId()))
                .set(Lead::getUpdateTime, new Date())
                .set(Lead::getUpdateBy, loginUser.getUsername())
                .set(Lead::getUpdateByName, loginUser.getRealName())
                .eq(Lead::getId, source.getId())
                .update();

        log.info("[Lead-Merge] target={} source={} customerCount={} ccCount={}",
                target.getId(), source.getId(), sourceCustomers.size(), sourceCC.size());
    }

    // ============================================================
    // 校验
    // ============================================================

    /**
     * 校验源线索（及其客户）的归属能否并入 target。
     *
     * 规则：源线索负责人必须为空，或与 target 负责人完全一致；否则拒绝并提示
     * "手机号 XXXX 已存在线索（负责人：YYY），无法添加"。
     *
     * 该规则对管理员同样生效——跨负责人合并必须先做正式转移，由销售拍板，
     * 不能通过"加手机号"暗自抢走他人客户。
     *
     * @param contactDescOrNull 入参的通信号描述（手机号/openId/微信号），用于错误文案；
     *                          旧 mergeLead 路径无入参时传 null，由 source 自身字段兜底。
     */
    private void ensureAllSourcesCanBeMerged(Lead target, List<Long> sourceLeadIds,
                                              LoginUser loginUser, String contactDescOrNull) {
        for (Long sid : sourceLeadIds) {
            Lead source = leadManager.getById(sid);
            if (source == null || GlobalConstant.Number.ONE_INT == orZero(source.getDeleted())) continue;
            // 1) 源线索负责人 vs 目标负责人
            if (!ownerEqualsTarget(source.getOwnerUser(), target)) {
                throw new LeadMergeException(buildOwnerMismatchMsg(
                        contactDescOrNull != null ? contactDescOrNull : describeLeadContact(source),
                        source.getOwnerUserName(),
                        target.getOwnerUserName()));
            }
            // 2) 源线索名下所有客户的 ownerUser
            List<CustomerInfo> customers = customerInfoManager.lambdaQuery()
                    .eq(CustomerInfo::getLeadId, source.getId())
                    .list();
            for (CustomerInfo c : customers) {
                if (!ownerEqualsTarget(c.getOwnerUser(), target)) {
                    throw new LeadMergeException(String.format(
                            "客户 [%s] 已被销售 [%s] 跟进，与本线索负责人（%s）不一致，无法合并",
                            nvl(c.getCustomerName()),
                            nvl(c.getOwnerUserName()),
                            displayOwner(target.getOwnerUserName())));
                }
            }
        }
    }

    /** 源 ownerUser 为空、或与 target 负责人精确匹配则视为可并入 */
    private boolean ownerEqualsTarget(String sourceOwner, Lead target) {
        if (StringUtils.isBlank(sourceOwner)) return true;
        return Objects.equals(sourceOwner, target.getOwnerUser());
    }

    /** 文案：手机号 XXXX 已存在线索（负责人：YYY），与本线索负责人（ZZZ）不一致，无法添加 */
    private String buildOwnerMismatchMsg(String contactDesc, String sourceOwnerName, String targetOwnerName) {
        return String.format("%s 已存在线索（负责人：%s），与本线索负责人（%s）不一致，无法添加",
                contactDesc,
                displayOwner(sourceOwnerName),
                displayOwner(targetOwnerName));
    }

    private static String displayOwner(String name) {
        return StringUtils.isBlank(name) ? "无" : name;
    }

    /** 根据 req 入参取一段可读描述（addPhone 走它） */
    private String describeReqContact(AddLeadPhoneReq req) {
        if (req == null) return "该通信号";
        if (StringUtils.isNotBlank(req.getPhone()))  return "手机号 " + req.getPhone();
        if (StringUtils.isNotBlank(req.getOpenId())) return "openId " + req.getOpenId();
        if (StringUtils.isNotBlank(req.getWechat())) return "微信号 " + req.getWechat();
        return "该通信号";
    }

    /** 旧合并路径用 source 自身字段描述 */
    private String describeLeadContact(Lead source) {
        if (StringUtils.isNotBlank(source.getPhone()))  return "手机号 " + source.getPhone();
        if (StringUtils.isNotBlank(source.getWechat())) return "微信号 " + source.getWechat();
        if (StringUtils.isNotBlank(source.getOpenId())) return "openId " + source.getOpenId();
        return "线索 #" + source.getId();
    }

    private void ensureCurrentUserCanOperate(Lead target, LoginUser loginUser) {
        if (loginUser == null) {
            throw new LeadMergeException("未获取到登录用户");
        }
        if (loginUser.isAdmin()) return;
        if (StringUtils.isBlank(target.getOwnerUser())) {
            // 无负责人 lead 允许任何有权限的人合并
            return;
        }
        if (!Objects.equals(target.getOwnerUser(), loginUser.getUsername())) {
            throw new LeadMergeException("非目标线索负责人，无法操作");
        }
    }

    // ============================================================
    // 通用动作（G1–G6）
    // ============================================================

    /**
     * 把 source 的 phone/openId/wechat（主表 + 子表）全部转入 target 子表，
     * 转入时去重：(target, phone) / (target, open_id) 已存在则跳过。
     */
    private void migratePhoneRowsToTarget(Lead target, Lead source, LoginUser loginUser) {
        // 1) source 主表 phone/openId/wechat 当作一条迁移行
        upsertLeadPhone(target, source.getPhone(), source.getOpenId(), source.getWechat(),
                "merge_from_lead", source.getId(), null, loginUser);

        // 2) source 子表所有有效行
        List<LeadPhone> rows = leadPhoneManager.lambdaQuery()
                .eq(LeadPhone::getLeadId, source.getId())
                .list();
        for (LeadPhone r : rows) {
            upsertLeadPhone(target, r.getPhone(), r.getOpenId(), r.getWechat(),
                    "merge_from_lead", source.getId(), r.getRemark(), loginUser);
        }

        // 3) 把 source 自己的子表行软删
        leadPhoneManager.lambdaUpdate()
                .set(LeadPhone::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(LeadPhone::getUpdateTime, new Date())
                .eq(LeadPhone::getLeadId, source.getId())
                .update();
    }

    private void upsertLeadPhone(Lead target, String phone, String openId, String wechat,
                                  String source, Long sourceLeadId, String remark, LoginUser loginUser) {
        if (StringUtils.isBlank(phone) && StringUtils.isBlank(openId) && StringUtils.isBlank(wechat)) {
            return;
        }
        // 兼容则补全、冲突则新增：
        //  - 已有"兼容行"（每字段两边要么空、要么相等）且 new 能补 null 字段 → UPDATE 已有行
        //  - 完全一致 → 跳过
        //  - 没有兼容行（任一字段两边非空且不等）→ 插入新行
        // 体现"phone 与 openId 强绑定"：phone-only 行被 mini-program 带来 openId 时，会落回同一行。
        String np = StringUtils.isBlank(phone)  ? null : phone;
        String no = StringUtils.isBlank(openId) ? null : openId;
        String nw = StringUtils.isBlank(wechat) ? null : wechat;

        List<LeadPhone> rows = leadPhoneManager.lambdaQuery()
                .eq(LeadPhone::getLeadId, target.getId())
                .list();
        for (LeadPhone r : rows) {
            String rp = StringUtils.isBlank(r.getPhone())  ? null : r.getPhone();
            String ro = StringUtils.isBlank(r.getOpenId()) ? null : r.getOpenId();
            String rw = StringUtils.isBlank(r.getWechat()) ? null : r.getWechat();
            boolean compat = fieldCompat(rp, np) && fieldCompat(ro, no) && fieldCompat(rw, nw);
            if (!compat) continue;
            boolean exact = Objects.equals(rp, np) && Objects.equals(ro, no) && Objects.equals(rw, nw);
            if (exact) return;
            // 兼容且能补全 → update
            LeadPhone update = new LeadPhone();
            update.setId(r.getId());
            boolean dirty = false;
            if (rp == null && np != null) { update.setPhone(np);  dirty = true; }
            if (ro == null && no != null) { update.setOpenId(no); dirty = true; }
            if (rw == null && nw != null) { update.setWechat(nw); dirty = true; }
            if (dirty) {
                update.setUpdateTime(new Date());
                if (loginUser != null) {
                    update.setUpdateBy(loginUser.getUsername());
                    update.setUpdateByName(loginUser.getRealName());
                }
                leadPhoneManager.updateById(update);
            }
            return;
        }

        LeadPhone row = new LeadPhone();
        row.setTenantId(target.getTenantId());
        row.setLeadId(target.getId());
        row.setPhone(StringUtils.isBlank(phone) ? null : phone);
        row.setOpenId(StringUtils.isBlank(openId) ? null : openId);
        row.setWechat(StringUtils.isBlank(wechat) ? null : wechat);
        row.setIsPrimary(GlobalConstant.Number.ZERO_INT);
        row.setSource(source);
        row.setSourceLeadId(sourceLeadId);
        row.setRemark(remark);
        row.setCreateBy(loginUser != null ? loginUser.getUsername() : null);
        row.setCreateByName(loginUser != null ? loginUser.getRealName() : null);
        row.setCreateTime(new Date());
        leadPhoneManager.save(row);
    }

    private void moveLeadLogs(Long sourceLeadId, Long targetLeadId, Long targetTenantId) {
        leadLogManager.lambdaUpdate()
                .set(LeadLog::getLeadId, targetLeadId)
                .set(targetTenantId != null, LeadLog::getTenantId, targetTenantId)
                .eq(LeadLog::getLeadId, sourceLeadId)
                .update();
    }

    // ============================================================
    // 客户层动作
    // ============================================================

    private void rebindCustomerToTarget(CustomerInfo cs, Lead target, LoginUser loginUser) {
        Long originalLeadId = cs.getLeadId();
        String originalOwner = cs.getOwnerUser();
        Integer originalType = cs.getCustomerType();

        LambdaUpdateWrapper<CustomerInfo> w = Wrappers.lambdaUpdate();
        w.set(CustomerInfo::getLeadId, target.getId());
        w.set(CustomerInfo::getLeadName, target.getLeadName());
        w.set(CustomerInfo::getCustomerType, GlobalConstant.Number.ONE_INT);
        w.set(CustomerInfo::getTenantId, target.getTenantId());
        w.set(CustomerInfo::getUpdateBy, loginUser.getUsername());
        w.set(CustomerInfo::getUpdateByName, loginUser.getRealName());
        w.set(CustomerInfo::getUpdateTime, new Date());
        // owner 改为当前用户（合并方）
        w.set(CustomerInfo::getOwnerUser, loginUser.getUsername());
        w.set(CustomerInfo::getOwnerUserName, loginUser.getRealName());
        w.eq(CustomerInfo::getId, cs.getId());
        customerInfoManager.update(w);

        // 写客户日志
        String content = String.format("因线索合并 (L%d→L%d) 归并：原 leadId=%s 原负责人=%s 原类型=%s",
                originalLeadId, target.getId(),
                String.valueOf(originalLeadId),
                nvl(originalOwner),
                originalType == null ? "" : (originalType == GlobalConstant.Number.ONE_INT ? "私海" : "公海"));
        customerLogService.recordCustomerLog(cs.getId(), GlobalConstant.Number.ONE_INT,
                CUSTOMER_LOG_TYPE_MERGE, content, null, null, target.getTenantId());
    }

    private void migrateCustomerTenant(CustomerInfo cs, Long targetTenantId) {
        if (targetTenantId == null) return;
        if (Objects.equals(cs.getTenantId(), targetTenantId)) return;
        // customer_info 已在 rebindCustomerToTarget 中改了 tenantId。
        // 级联搬迁 customer_log 的 tenant_id（旧日志仍归属源租户，PC 端按租户查询会漏）
        customerLogManager.lambdaUpdate()
                .set(CustomerLog::getTenantId, targetTenantId)
                .eq(CustomerLog::getCustomerId, cs.getId())
                .update();
    }

    private void migrateCCTenant(CustomerCompany cc, Long targetTenantId) {
        if (targetTenantId == null) return;
        if (Objects.equals(cc.getTenantId(), targetTenantId)) return;

        // 1) 主表 CC
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getTenantId, targetTenantId)
                .eq(CustomerCompany::getId, cc.getId())
                .update();

        // 2) 子表中有 tenant_id 的：company_contact / company_employee
        //    （其余子表无 tenant_id 字段，按 company_id 关联即可，无需迁移）
        companyContactManager.lambdaUpdate()
                .set(CompanyContact::getTenantId, targetTenantId)
                .eq(CompanyContact::getCompanyId, cc.getId())
                .update();
        companyEmployeeManager.lambdaUpdate()
                .set(CompanyEmployee::getTenantId, targetTenantId)
                .eq(CompanyEmployee::getCompanyId, cc.getId())
                .update();
    }

    /**
     * 分支 1.x / 2.x：选定"无客户的源 lead 子表行"在子表里挂哪个客户。
     * 规则：
     *  1. 若 target 自身有 customer → 选最早创建的 target.customer
     *  2. 否则 → 选最早创建的 source.customer
     */
    private Long chooseDefaultCustomerId(Lead target, List<CustomerInfo> sourceCustomers) {
        // target.customers
        List<CustomerInfo> targetCustomers = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getLeadId, target.getId())
                .orderByAsc(CustomerInfo::getCreateTime)
                .list();
        if (!targetCustomers.isEmpty()) return targetCustomers.get(0).getId();

        if (sourceCustomers.isEmpty()) return null;
        return sourceCustomers.stream()
                .min(Comparator.comparing(CustomerInfo::getCreateTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(CustomerInfo::getId).orElse(null);
    }

    private Long chooseDefaultCustomerForOrphanSource(Lead target) {
        List<CustomerInfo> targetCustomers = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getLeadId, target.getId())
                .orderByAsc(CustomerInfo::getCreateTime)
                .list();
        return targetCustomers.isEmpty() ? null : targetCustomers.get(0).getId();
    }

    private void backfillDefaultCustomerOnNewlyMigratedRows(Lead target, Lead source, Long defaultCustomerId) {
        if (defaultCustomerId == null) return;
        leadPhoneManager.lambdaUpdate()
                .set(LeadPhone::getDefaultCustomerId, defaultCustomerId)
                .eq(LeadPhone::getLeadId, target.getId())
                .eq(LeadPhone::getSourceLeadId, source.getId())
                .isNull(LeadPhone::getDefaultCustomerId)
                .update();
    }

    // ============================================================
    // 合并尾声 - 家族广播
    // ============================================================

    /**
     * 合并尾声广播。
     *
     * 简单遍历"每个 CC 自我广播"是顺序敏感的（后写的覆盖前写的），多 CC 字段不一致时
     * 收敛结果不确定。改为：
     *   1) 按 (taxNumber) 把 target 家族 CC 分组
     *   2) 每组内"取每个字段在组内 update_time 最新的非空值"，作为权威值
     *   3) 选组内任一 CC 作为源，调 broadcaster.broadcast() 把权威字段集合一次性广播
     *
     * 这样无论组内 CC 写顺序如何，最终结果都收敛到"最新非空值"。
     */
    private void broadcastFamilyAfterMerge(Lead target) {
        List<Long> customerIds = customerInfoManager.lambdaQuery()
                .select(CustomerInfo::getId)
                .eq(CustomerInfo::getLeadId, target.getId())
                .list().stream().map(CustomerInfo::getId).toList();
        if (customerIds.isEmpty()) return;

        List<CustomerCompany> familyCC = customerCompanyManager.lambdaQuery()
                .in(CustomerCompany::getCustomerId, customerIds)
                .list();
        if (CollUtil.isEmpty(familyCC)) return;

        // 按 tax_number 分组
        Map<String, List<CustomerCompany>> byTax = new HashMap<>();
        for (CustomerCompany cc : familyCC) {
            if (cc.getTaxNumber() == null || cc.getTaxNumber().isEmpty()) continue;
            byTax.computeIfAbsent(cc.getTaxNumber(), k -> new ArrayList<>()).add(cc);
        }

        String operator = currentOperator();
        for (List<CustomerCompany> group : byTax.values()) {
            if (group.size() < 2) continue; // 组内只有 1 条无需广播
            // 取每个字段的"最新非空值"
            Map<String, Object> authoritative = pickAuthoritativeFields(group);
            if (authoritative.isEmpty()) continue;
            // 选 update_time 最新的 CC 作为广播源
            CustomerCompany src = group.stream()
                    .max(Comparator.comparing(CustomerCompany::getUpdateTime,
                            Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(group.get(0));
            customerCompanyBroadcaster.broadcast(src.getId(), authoritative, operator);
        }
    }

    /**
     * 在一组同税号 CC 中，按字段取"最新非空值"。
     * 同字段所有 CC 都为空时跳过；任一非空则取 update_time 最新的非空值。
     */
    private Map<String, Object> pickAuthoritativeFields(List<CustomerCompany> group) {
        // 把"可广播字段名 → 字段取值函数"准备好
        List<FieldAccessor> accessors = List.of(
                new FieldAccessor("companyName",       CustomerCompany::getCompanyName),
                new FieldAccessor("legalName",         CustomerCompany::getLegalName),
                new FieldAccessor("legalIdCard",       CustomerCompany::getLegalIdCard),
                new FieldAccessor("legalPhone",        CustomerCompany::getLegalPhone),
                new FieldAccessor("establishedDate",   CustomerCompany::getEstablishedDate),
                new FieldAccessor("regStatus",         CustomerCompany::getRegStatus),
                new FieldAccessor("registeredAddress", CustomerCompany::getRegisteredAddress),
                new FieldAccessor("taxDistrict",       CustomerCompany::getTaxDistrict),
                new FieldAccessor("province",          CustomerCompany::getProvince),
                new FieldAccessor("companyType",       CustomerCompany::getCompanyType),
                new FieldAccessor("isIndividual",      CustomerCompany::getIsIndividual),
                new FieldAccessor("isDoubleExemption", CustomerCompany::getIsDoubleExemption),
                // 类 B 字段：升级方向通过 MAX；降级方向由 broadcaster 守门
                new FieldAccessor("isVerifyTax",       CustomerCompany::getIsVerifyTax),
                new FieldAccessor("isInvoicing",       CustomerCompany::getIsInvoicing),
                new FieldAccessor("isBookkeeping",     CustomerCompany::getIsBookkeeping),
                new FieldAccessor("companyStatus",     CustomerCompany::getCompanyStatus),
                new FieldAccessor("statusRemark",      CustomerCompany::getStatusRemark)
        );
        Set<String> flagFields = Set.of("isVerifyTax", "isInvoicing", "isBookkeeping");

        Map<String, Object> result = new LinkedHashMap<>();
        for (FieldAccessor fa : accessors) {
            Object v;
            if (flagFields.contains(fa.name)) {
                // 标志位升级：取 MAX
                Integer max = group.stream()
                        .map(cc -> (Integer) fa.fn.apply(cc))
                        .filter(Objects::nonNull)
                        .max(Integer::compareTo).orElse(null);
                v = max;
            } else {
                // 取 update_time 最新的非空值
                v = group.stream()
                        .filter(cc -> fa.fn.apply(cc) != null
                                && !(fa.fn.apply(cc) instanceof String s && s.isEmpty()))
                        .max(Comparator.comparing(CustomerCompany::getUpdateTime,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                        .map(fa.fn).orElse(null);
            }
            if (v != null) result.put(fa.name, v);
        }
        return result;
    }

    /** 字段访问器：把字段名与 getter 关联起来 */
    private record FieldAccessor(String name, java.util.function.Function<CustomerCompany, Object> fn) {}

    // ============================================================
    // 合并历史快照
    // ============================================================

    private void writeMergeHistory(Lead target, Lead source,
                                    List<CustomerInfo> sourceCustomers,
                                    List<CustomerCompany> sourceCC,
                                    LoginUser loginUser) {
        LeadMergeHistory h = new LeadMergeHistory();
        h.setTenantId(target.getTenantId());
        h.setTargetLeadId(target.getId());
        h.setSourceLeadId(source.getId());
        h.setSourceLeadSnapshot(JSON.toJSONString(source));
        h.setCustomerSnapshot(JSON.toJSONString(sourceCustomers));
        h.setCcSnapshot(JSON.toJSONString(sourceCC));
        h.setOperator(loginUser.getUsername());
        h.setOperatorName(loginUser.getRealName());
        h.setCreateTime(new Date());
        leadMergeHistoryManager.save(h);
    }

    // ============================================================
    // 旧 mergeLead 兼容：mergeResult 字段覆盖
    // ============================================================

    private void updateTargetByMergeResult(Long targetLeadId, Map<String, Object> mr, Long tenantId) {
        Lead update = new Lead();
        update.setId(targetLeadId);
        if (mr.get("leadName") != null)      update.setLeadName(String.valueOf(mr.get("leadName")));
        if (mr.get("type") != null)          update.setType(String.valueOf(mr.get("type")));
        if (mr.get("contactName") != null)   update.setContactName(String.valueOf(mr.get("contactName")));
        if (mr.get("phone") != null)         update.setPhone(String.valueOf(mr.get("phone")));
        if (mr.get("wechat") != null)        update.setWechat(String.valueOf(mr.get("wechat")));
        if (mr.get("email") != null)         update.setEmail(String.valueOf(mr.get("email")));
        if (mr.get("companyName") != null)   update.setCompanyName(String.valueOf(mr.get("companyName")));
        if (mr.get("source") != null)        update.setSource(String.valueOf(mr.get("source")));
        if (mr.get("status") != null)        update.setStatus(Integer.valueOf(mr.get("status").toString()));
        if (mr.get("ownerUser") != null)     update.setOwnerUser(String.valueOf(mr.get("ownerUser")));
        if (mr.get("ownerUserName") != null) update.setOwnerUserName(String.valueOf(mr.get("ownerUserName")));
        if (mr.get("remark") != null)        update.setRemark(String.valueOf(mr.get("remark")));
        if (mr.get("openId") != null)        update.setOpenId(String.valueOf(mr.get("openId")));
        update.setTenantId(tenantId);
        leadManager.updateById(update);
    }

    // ============================================================
    // 工具
    // ============================================================

    private Lead mustGetLead(Long id) {
        Lead l = leadManager.getById(id);
        if (l == null || GlobalConstant.Number.ONE_INT == orZero(l.getDeleted())) {
            throw new LeadMergeException("线索不存在或已删除：" + id);
        }
        return l;
    }

    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            throw new LeadMergeException("未获取到登录用户");
        }
        return (LoginUser) auth.getPrincipal();
    }

    private String currentOperator() {
        try {
            return getLoginUser().getUsername();
        } catch (Exception e) {
            return "system";
        }
    }

    private static int orZero(Integer v) { return v == null ? 0 : v; }
    private static String nvl(String s)   { return s == null ? "" : s; }

    /** 字段兼容判定：两端要么有一方为空，要么相等 */
    private static boolean fieldCompat(String a, String b) {
        if (a == null || b == null) return true;
        return a.equals(b);
    }

    private static String mergedRemark(String original, Long targetId) {
        String tail = String.format("[merged_into=%d]", targetId);
        if (StringUtils.isBlank(original)) return tail;
        return original + " " + tail;
    }
}
