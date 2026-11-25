package com.qykj.finmaster.order.domain.req;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-11-21
 * @Description
 */
@Data
public class RefundCancelTaxReq implements Serializable {
    private static final long serialVersionUID = -7121045678854082836L;

    /**
     * 订单号
     */
    private String orderNo;

}