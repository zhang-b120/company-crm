package com.qykj.finmaster.payment.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-06-19
 * @Description
 */
@Data
public class JsapiRefundReq implements Serializable {

    private static final long serialVersionUID = 5414356684312004531L;
    /**
     * 支付名称
     */
    private String paymentName;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 订单金额
     */
    private BigDecimal totalAmount;
}