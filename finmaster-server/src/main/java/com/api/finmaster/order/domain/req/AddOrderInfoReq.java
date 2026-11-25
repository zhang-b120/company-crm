package com.qykj.finmaster.order.domain.req;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AddOrderInfoReq implements Serializable {
    private static final long serialVersionUID = -1204818620997040837L;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 折扣比例 */
    private Integer discount;

    /** 最终金额 */
    private BigDecimal finalAmount;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 税后金额 */
    private BigDecimal afterTaxAmount;

    /** 已付金额 */
    private BigDecimal paidAmount;

    /** 订单状态（PENDING待付款、PAID已付款、CANCELLED已取消） */
    private String status;

    /** 附件列表 */
    private String attachments;

    /** 备注 */
    private String remark;

    /**
     * 付款方式-字典
     */
    private String paymentType;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 线索月份
     */
    private String leadMonth;

    /** 订单明细列表 */
    private List<OrderDetailInfoReq> orderDetails;
}
