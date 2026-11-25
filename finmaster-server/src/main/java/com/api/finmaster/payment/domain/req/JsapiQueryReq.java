package com.qykj.finmaster.payment.domain.req;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-17
 * @Description
 */
@Data
public class JsapiQueryReq implements Serializable {
    private static final long serialVersionUID = -6625603076094716867L;

    /**
     * 支付名称
     */
    private String paymentName;

    /**
     * 订单编号
     */
    private String orderNo;
}