package com.qykj.finmaster.customer.customerInfo.domain.req;/* Copyright © 2020 Yuech and/or its affiliates. All rights reserved. */

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-05-16
 * @Description
 */
@Data
public class UaGetCustomerInfoReq implements Serializable {

    private static final long serialVersionUID = -3923079152820014719L;
    /**
     * 微信openId
     */
    private String openId;
}
