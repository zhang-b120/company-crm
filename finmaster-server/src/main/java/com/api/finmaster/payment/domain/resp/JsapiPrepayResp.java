package com.qykj.finmaster.payment.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-17
 * @Description
 */
@Data
public class JsapiPrepayResp implements Serializable {
    private static final long serialVersionUID = 5738019061590572584L;

    /**
     * 微信支付下单ID
     */
    private String prepayId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * AppId
     */
    private String appId;

    /**
     * Unix 时间戳 秒级
     */
    private String timeStamp;
    /**
     * 32位随机数
     */
    private String nonceStr;

    /**
     * 签名类型 固定值RSA
     */
    private String signType;

    /**
     * 支付签名
     */
    private String paySign;
}