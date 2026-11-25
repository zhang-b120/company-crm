package com.qykj.finmaster.order.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AddRefundOrderReq implements Serializable {

    private static final long serialVersionUID = 2915567032404620850L;

    /**
     * 原订单号
     */
    private String sourceOrderNo;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 退款金额
     */
    private BigDecimal totalAmount;

    /**
     * 最终金额
     */
    private BigDecimal finalAmount;

    /**
     * 备注
     */
    private String remark;

    /**
     * 线索月份
     */
    private String leadMonth;

    /**
     * 订单明细列表
     */
    private List<OrderDetailInfoReq> orderDetails;

    private String bankName;

    private String bankAccount;

    private String bankAccountName;

    /** 退款原因（必填，将随退款工单一起流转） */
    private String refundReason;

    /** 退款凭证图片 URL 列表（JSON 数组字符串：客户聊天截图等） */
    private String refundImages;

    /** 退款方式 MINI_PROGRAM 小程序退款 / BANK_TRANSFER 银行转账 */
    private String refundMethod;

    /** 审核人 username（退款工单第一节点 REFUND_REVIEW 的负责人） */
    private String reviewerUser;

}
