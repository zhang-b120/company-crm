package com.qykj.finmaster.customer.company.service;

import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.CustomerInfoManager;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.manager.OrderManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 家族字段降级守门：
 *   - is_verify_tax / is_invoicing / is_bookkeeping 1→0
 *   - company_status COOPERATING → CANCELLED
 *
 * 守门规则：如家族内除当前 CC 外、其它 CC 关联的客户仍有"未退款的 PAID NORMAL 订单"，
 * 则不允许将状态降级广播过去（保留各 CC 自己的状态）。
 *
 * 升级方向（0→1 或 → COOPERATING / DEREGISTERED）一律通过。
 */
@Service
public class FamilyGuardService {

    @Autowired
    private CustomerCompanyManager customerCompanyManager;

    @Autowired
    private CustomerInfoManager customerInfoManager;

    @Autowired
    private OrderManager orderManager;

    /**
     * 判定是否允许把 (field, newValue) 广播到 family。
     * 仅类 B 字段调用。
     */
    public boolean passesGuard(String field,
                                Object newValue,
                                CustomerCompanyBroadcaster.FamilyKey key,
                                Long sourceCompanyId) {
        return switch (field) {
            case "isVerifyTax", "isInvoicing", "isBookkeeping" -> guardFlagDowngrade(newValue, key, sourceCompanyId);
            case "companyStatus" -> guardCompanyStatus(newValue, key, sourceCompanyId);
            case "statusRemark"  -> true; // 跟随 companyStatus，单独 update 时直接放行
            default -> true;
        };
    }

    /**
     * 1→0 时检查家族其它 CC 是否仍有"未退款的 PAID NORMAL 订单"
     */
    private boolean guardFlagDowngrade(Object newValue, CustomerCompanyBroadcaster.FamilyKey key, Long sourceCompanyId) {
        Integer v = toInt(newValue);
        if (v == null) return true;
        if (v == GlobalConstant.Number.ONE_INT) return true;          // 升级方向放行
        return !familyHasActiveOrder(key, sourceCompanyId);
    }

    /**
     * companyStatus:
     *  DEREGISTERED / COOPERATING → 升级 / 客观状态，直接广播
     *  CANCELLED → 视为降级，需守门
     */
    private boolean guardCompanyStatus(Object newValue, CustomerCompanyBroadcaster.FamilyKey key, Long sourceCompanyId) {
        if (newValue == null) return true;
        String s = newValue.toString();
        if ("CANCELLED".equalsIgnoreCase(s)) {
            return !familyHasActiveOrder(key, sourceCompanyId);
        }
        return true;
    }

    /**
     * 家族（同 leadId + 同 tax_number）除 sourceCompanyId 外，是否还存在客户上有 PAID NORMAL 订单且未退款。
     */
    private boolean familyHasActiveOrder(CustomerCompanyBroadcaster.FamilyKey key, Long sourceCompanyId) {
        // 1) 取同 leadId 的客户 ID 列表
        List<Long> customerIds = customerInfoManager.lambdaQuery()
                .select(CustomerInfo::getId)
                .eq(CustomerInfo::getLeadId, key.leadId)
                .list().stream().map(CustomerInfo::getId).toList();
        if (customerIds.isEmpty()) return false;

        // 2) 找家族内同税号但非 sourceCompanyId 的 CC
        List<Long> familyCcIds = customerCompanyManager.lambdaQuery()
                .select(CustomerCompany::getId, CustomerCompany::getCustomerId)
                .in(CustomerCompany::getCustomerId, customerIds)
                .eq(CustomerCompany::getTaxNumber, key.taxNumber)
                .ne(sourceCompanyId != null, CustomerCompany::getId, sourceCompanyId)
                .list().stream().map(CustomerCompany::getId).toList();
        if (familyCcIds.isEmpty()) return false;

        // 3) 这些 CC 关联的 customer 上是否有 PAID NORMAL 且未退款的订单
        //    判定按客户维度（与现有 isTransaction SQL 对齐）
        List<Long> familyCustomerIds = customerCompanyManager.lambdaQuery()
                .select(CustomerCompany::getCustomerId)
                .in(CustomerCompany::getId, familyCcIds)
                .list().stream().map(CustomerCompany::getCustomerId).distinct().toList();
        if (familyCustomerIds.isEmpty()) return false;

        return orderManager.lambdaQuery()
                .in(Order::getCustomerId, familyCustomerIds)
                .eq(Order::getStatus, "PAID")
                .eq(Order::getOrderType, "NORMAL")
                .exists();
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s)  return s.isBlank() ? null : Integer.parseInt(s);
        if (v instanceof Boolean b) return b ? GlobalConstant.Number.ONE_INT : GlobalConstant.Number.ZERO_INT;
        return null;
    }
}
