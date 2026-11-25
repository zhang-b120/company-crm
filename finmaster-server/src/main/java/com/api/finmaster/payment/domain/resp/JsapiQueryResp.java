package com.qykj.finmaster.payment.domain.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-17
 * @Description
 */
@Data
public class JsapiQueryResp implements Serializable {
    private static final long serialVersionUID = -6745429851836775601L;

    /**
     * 交易状态
     */
    private String tradeStatus;

}