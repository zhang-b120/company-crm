package com.qykj.finmaster.history;

import java.time.LocalDate;

/**
 * 历史订单用于生成合同的明细记录
 */
public class HistoryOrderDetailRecord {

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 订单明细类型 NEW / RENEW
     */
    private String orderDetailType;

    /**
     * 订单明细ID
     */
    private Long orderDetailId;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 产品生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 产品结束日期
     */
    private LocalDate endDate;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public String getOrderDetailType() {
        return orderDetailType;
    }

    public void setOrderDetailType(String orderDetailType) {
        this.orderDetailType = orderDetailType;
    }

    public Long getOrderDetailId() {
        return orderDetailId;
    }

    public void setOrderDetailId(Long orderDetailId) {
        this.orderDetailId = orderDetailId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}

