package com.qykj.finmaster.payment.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-19
 * @Description
 */
@Data
public class JsapiCloseReq implements Serializable {
    private static final long serialVersionUID = 4988231519575784853L;

    /**
     * 支付名称
     */
    private String paymentName;

    /**
     * 订单编号
     */
    private String orderNo;

}