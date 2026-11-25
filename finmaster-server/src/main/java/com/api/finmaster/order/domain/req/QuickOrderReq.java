package com.qykj.finmaster.order.domain.req;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 快速建单请求：一步完成客户+公司+订单的创建
 */
@Data
public class QuickOrderReq {

    /** 线索ID（可选，提供则自动转化线索为私海客户） */
    private Long leadId;

    /** 私海客户ID（可选，已有客户时直接使用） */
    private Long customerId;

    /** 联系人姓名（创建新客户时使用） */
    private String contactName;

    /** 联系人手机号（创建新客户时使用） */
    private String phone;

    /** 公司名称（必填，用于查找或创建公司） */
    private String companyName;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal finalAmount;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 税后金额 */
    private BigDecimal afterTaxAmount;

    /** 已付金额 */
    private BigDecimal paidAmount;

    /** 折扣 */
    private Integer discount;

    /** 订单状态：PENDING/PAID */
    private String status;

    /** 支付方式（字典） */
    private String paymentType;

    /** 订单明细 */
    private List<OrderDetailInfoReq> orderDetails;

    /** 备注 */
    private String remark;
}
