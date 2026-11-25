package com.qykj.finmaster.payment.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-06-12
 * @Description
 */
@Data
public class JsapiPrepayReq implements Serializable {
    private static final long serialVersionUID = 5413888624235376590L;

    /**
     * 支付名称
     */
    private String paymentName;

    /**
     * 用户OpenId
     */
    private String openId;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 产品Id
     */
    private Long productId;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 商品描述
     */
    private String description;
}