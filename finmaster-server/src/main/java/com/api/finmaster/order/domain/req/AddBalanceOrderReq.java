package com.qykj.finmaster.order.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 新增补款订单请求
 * 补款订单记录原订单号（source_order_no），明细记录原明细ID（source_detail_id），业务类型强制为补款
 */
@Data
public class AddBalanceOrderReq implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原订单号 */
    private String sourceOrderNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 最终金额 */
    private BigDecimal finalAmount;

    /** 备注 */
    private String remark;

    /** 线索月份 */
    private String leadMonth;

    /** 订单状态 */
    private String status;

    /** 支付方式 */
    private String paymentType;

    /** 支付时间 */
    private String payTime;

    /** 订单明细列表（含 sourceDetailId，业务强制补款由后端设置） */
    private List<OrderDetailInfoReq> orderDetails;
}
