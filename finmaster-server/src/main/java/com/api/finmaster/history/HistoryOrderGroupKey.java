package com.qykj.finmaster.history;

import java.util.Objects;

/**
 * 历史订单生成合同时的分组键
 * 维度：tenantId + companyId + businessId + orderDetailType
 */
public class HistoryOrderGroupKey {

    private final Long tenantId;
    private final Long companyId;
    private final Long businessId;
    /**
     * 订单明细类型 NEW / RENEW
     */
    private final String orderDetailType;

    public HistoryOrderGroupKey(Long tenantId, Long companyId, Long businessId, String orderDetailType) {
        this.tenantId = tenantId;
        this.companyId = companyId;
        this.businessId = businessId;
        this.orderDetailType = orderDetailType;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public String getOrderDetailType() {
        return orderDetailType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HistoryOrderGroupKey)) {
            return false;
        }
        HistoryOrderGroupKey that = (HistoryOrderGroupKey) o;
        return Objects.equals(tenantId, that.tenantId)
                && Objects.equals(companyId, that.companyId)
                && Objects.equals(businessId, that.businessId)
                && Objects.equals(orderDetailType, that.orderDetailType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, companyId, businessId, orderDetailType);
    }
}

