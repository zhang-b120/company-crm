package com.qykj.finmaster.customer.company.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.CustomerInfoManager;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * customer_company 字段家族广播服务。
 *
 * 家族键 = (customer_info.lead_id, customer_company.tax_number)
 * 在家族内的所有 CC 视为"同一真人 + 同一真实公司"的不同登录视角。
 *
 * 字段分三类：
 *  - 类 A 直接广播：客观工商属性
 *  - 类 B 守门广播：业务状态（升级方向无守门；降级方向需检查家族业务依赖）
 *  - 类 C 禁止广播：每个 CC 独立字段（安全网拒绝）
 */
@Service
public class CustomerCompanyBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(CustomerCompanyBroadcaster.class);

    /** 类 A：客观工商属性，无守门 */
    public static final Set<String> BROADCAST_PURE = Set.of(
            "companyName", "legalName", "legalIdCard", "legalPhone",
            "establishedDate", "regStatus", "registeredAddress",
            "taxDistrict", "province", "companyType",
            "isIndividual", "isDoubleExemption"
    );

    /** 类 B：业务状态，降级方向需守门 */
    public static final Set<String> BROADCAST_GUARDED = Set.of(
            "isVerifyTax", "isInvoicing", "isBookkeeping",
            "companyStatus", "statusRemark"
    );

    /** 类 C：每个 CC 独立，禁止广播 */
    public static final Set<String> NEVER_BROADCAST = Set.of(
            "id", "tenantId", "customerId", "openid", "taxNumber",
            "enabled", "experienceAccountSetId",
            "createBy", "createByName", "createTime",
            "updateBy", "updateByName", "updateTime", "deleted"
    );

    @Autowired
    private CustomerCompanyManager customerCompanyManager;

    @Autowired
    private CustomerInfoManager customerInfoManager;

    @Autowired
    private FamilyGuardService familyGuardService;

    /**
     * 在家族内广播指定字段的最新值。
     *
     * @param sourceCompanyId 触发字段变化的 CC（不会被自身广播覆盖）
     * @param dirtyFields     本次实际改动的字段 → 新值；调用方负责只传"真正改了的"
     * @param operator        操作人 username（可为空）
     */
    @Transactional
    public void broadcast(Long sourceCompanyId, Map<String, Object> dirtyFields, String operator) {
        if (sourceCompanyId == null || dirtyFields == null || dirtyFields.isEmpty()) {
            return;
        }
        FamilyKey key = resolveFamilyKey(sourceCompanyId);
        if (key == null || key.leadId == null || StringUtils.isBlank(key.taxNumber)) {
            // 没有家族键（leadId 为空或税号空）→ 不广播，仅依赖调用方对源 CC 自身的 update
            return;
        }
        List<Long> targetIds = findFamilyCCIds(key.leadId, key.taxNumber, sourceCompanyId);
        if (CollUtil.isEmpty(targetIds)) {
            return;
        }

        Map<String, Object> safeUpdate = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : dirtyFields.entrySet()) {
            String fieldName = entry.getKey();
            Object newValue = entry.getValue();
            if (NEVER_BROADCAST.contains(fieldName)) {
                throw new IllegalArgumentException("字段 [" + fieldName + "] 禁止跨家族广播");
            }
            if (BROADCAST_PURE.contains(fieldName)) {
                safeUpdate.put(fieldName, newValue);
            } else if (BROADCAST_GUARDED.contains(fieldName)) {
                if (familyGuardService.passesGuard(fieldName, newValue, key, sourceCompanyId)) {
                    safeUpdate.put(fieldName, newValue);
                } else {
                    log.info("[CC-Broadcast] guard 阻止：field={} newValue={} leadId={} taxNumber={}",
                            fieldName, newValue, key.leadId, key.taxNumber);
                }
            }
            // 其它字段忽略
        }
        if (safeUpdate.isEmpty()) {
            return;
        }
        applyUpdate(targetIds, safeUpdate, operator);
        log.info("[CC-Broadcast] sourceCC={} targets={} fields={} operator={}",
                sourceCompanyId, targetIds, safeUpdate.keySet(), operator);
    }

    /**
     * 从源 CC 把所有"可广播字段"再同步一遍（合并尾声 / 数据校准用）。
     */
    public void broadcastSelfToFamily(Long sourceCompanyId, String operator) {
        CustomerCompany src = customerCompanyManager.getById(sourceCompanyId);
        if (src == null) return;
        Map<String, Object> all = extractBroadcastableFields(src);
        broadcast(sourceCompanyId, all, operator);
    }

    /**
     * 仅广播一个字段（便利方法）。
     */
    public void broadcastSingleField(Long sourceCompanyId, String fieldName, Object newValue, String operator) {
        if (sourceCompanyId == null || fieldName == null) return;
        broadcast(sourceCompanyId, Map.of(fieldName, newValue == null ? "" : newValue), operator);
    }

    /**
     * 批量广播：对多个 sourceCompanyId 各自做 broadcastSelfToFamily。
     */
    public void broadcastSelfToFamilyBatch(Collection<Long> sourceCompanyIds, String operator) {
        if (CollUtil.isEmpty(sourceCompanyIds)) return;
        for (Long id : sourceCompanyIds) {
            broadcastSelfToFamily(id, operator);
        }
    }

    /** 取家族键 */
    private FamilyKey resolveFamilyKey(Long companyId) {
        CustomerCompany cc = customerCompanyManager.getById(companyId);
        if (cc == null) return null;
        if (StringUtils.isBlank(cc.getTaxNumber())) return null;
        if (cc.getCustomerId() == null) return null;
        CustomerInfo customer = customerInfoManager.getById(cc.getCustomerId());
        if (customer == null) return null;
        return new FamilyKey(customer.getLeadId(), cc.getTaxNumber());
    }

    /** 找家族内其它 CC.id 列表 */
    private List<Long> findFamilyCCIds(Long leadId, String taxNumber, Long excludeId) {
        // 同 leadId 的 customer_info → 命中其 customer_company 中同税号者
        List<Long> customerIds = customerInfoManager.lambdaQuery()
                .select(CustomerInfo::getId)
                .eq(CustomerInfo::getLeadId, leadId)
                .list().stream().map(CustomerInfo::getId).toList();
        if (CollUtil.isEmpty(customerIds)) return Collections.emptyList();

        return customerCompanyManager.lambdaQuery()
                .select(CustomerCompany::getId)
                .in(CustomerCompany::getCustomerId, customerIds)
                .eq(CustomerCompany::getTaxNumber, taxNumber)
                .ne(excludeId != null, CustomerCompany::getId, excludeId)
                .list().stream().map(CustomerCompany::getId).toList();
    }

    /** 批量 update 选中 CC 的字段值 */
    private void applyUpdate(List<Long> targetIds, Map<String, Object> fields, String operator) {
        LambdaUpdateWrapper<CustomerCompany> wrapper = Wrappers.lambdaUpdate();
        wrapper.in(CustomerCompany::getId, targetIds);
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            applyField(wrapper, entry.getKey(), entry.getValue());
        }
        wrapper.set(CustomerCompany::getUpdateTime, new Date());
        if (StringUtils.isNotBlank(operator)) {
            wrapper.set(CustomerCompany::getUpdateBy, operator);
        }
        customerCompanyManager.update(wrapper);
    }

    /** 把字符串字段名映射到 lambda set 调用（避免反射，显式枚举可广播字段） */
    private void applyField(LambdaUpdateWrapper<CustomerCompany> w, String field, Object v) {
        switch (field) {
            case "companyName"        -> w.set(CustomerCompany::getCompanyName, v);
            case "legalName"          -> w.set(CustomerCompany::getLegalName, v);
            case "legalIdCard"        -> w.set(CustomerCompany::getLegalIdCard, v);
            case "legalPhone"         -> w.set(CustomerCompany::getLegalPhone, v);
            case "establishedDate"    -> w.set(CustomerCompany::getEstablishedDate, v);
            case "regStatus"          -> w.set(CustomerCompany::getRegStatus, v);
            case "registeredAddress"  -> w.set(CustomerCompany::getRegisteredAddress, v);
            case "taxDistrict"        -> w.set(CustomerCompany::getTaxDistrict, v);
            case "province"           -> w.set(CustomerCompany::getProvince, v);
            case "companyType"        -> w.set(CustomerCompany::getCompanyType, v);
            case "isIndividual"       -> w.set(CustomerCompany::getIsIndividual, toInt(v));
            case "isDoubleExemption"  -> w.set(CustomerCompany::getIsDoubleExemption, toInt(v));
            case "isVerifyTax"        -> w.set(CustomerCompany::getIsVerifyTax, toInt(v));
            case "isInvoicing"        -> w.set(CustomerCompany::getIsInvoicing, toInt(v));
            case "isBookkeeping"      -> w.set(CustomerCompany::getIsBookkeeping, toInt(v));
            case "companyStatus"      -> w.set(CustomerCompany::getCompanyStatus, v);
            case "statusRemark"       -> w.set(CustomerCompany::getStatusRemark, v);
            default -> throw new IllegalArgumentException("不支持广播的字段：" + field);
        }
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Number n)  return n.intValue();
        if (v instanceof String s)  return s.isBlank() ? null : Integer.parseInt(s);
        if (v instanceof Boolean b) return b ? GlobalConstant.Number.ONE_INT : GlobalConstant.Number.ZERO_INT;
        throw new IllegalArgumentException("无法转 Integer: " + v.getClass());
    }

    /** 把源 CC 上的所有可广播字段都打包成 Map（用于合并尾声同步整批） */
    private Map<String, Object> extractBroadcastableFields(CustomerCompany cc) {
        Map<String, Object> map = new LinkedHashMap<>();
        putIfNotNull(map, "companyName", cc.getCompanyName());
        putIfNotNull(map, "legalName", cc.getLegalName());
        putIfNotNull(map, "legalIdCard", cc.getLegalIdCard());
        putIfNotNull(map, "legalPhone", cc.getLegalPhone());
        putIfNotNull(map, "establishedDate", cc.getEstablishedDate());
        putIfNotNull(map, "regStatus", cc.getRegStatus());
        putIfNotNull(map, "registeredAddress", cc.getRegisteredAddress());
        putIfNotNull(map, "taxDistrict", cc.getTaxDistrict());
        putIfNotNull(map, "province", cc.getProvince());
        putIfNotNull(map, "companyType", cc.getCompanyType());
        putIfNotNull(map, "isIndividual", cc.getIsIndividual());
        putIfNotNull(map, "isDoubleExemption", cc.getIsDoubleExemption());
        putIfNotNull(map, "isVerifyTax", cc.getIsVerifyTax());
        putIfNotNull(map, "isInvoicing", cc.getIsInvoicing());
        putIfNotNull(map, "isBookkeeping", cc.getIsBookkeeping());
        putIfNotNull(map, "companyStatus", cc.getCompanyStatus());
        putIfNotNull(map, "statusRemark", cc.getStatusRemark());
        return map;
    }

    private void putIfNotNull(Map<String, Object> m, String k, Object v) {
        if (ObjectUtils.isNotEmpty(v)) m.put(k, v);
    }

    /**
     * 家族键
     */
    public static final class FamilyKey {
        public final Long leadId;
        public final String taxNumber;
        public FamilyKey(Long leadId, String taxNumber) {
            this.leadId = leadId;
            this.taxNumber = taxNumber;
        }
    }
}
