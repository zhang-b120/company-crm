package com.qykj.finmaster.customer.customerInfo.domain.resp;/* Copyright © 2020 Yuech and/or its affiliates. All rights reserved. */

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-05-16
 * @Description
 */
@Data
public class UaGetCustomerInfoResp implements Serializable {

    private static final long serialVersionUID = -4238597926393826888L;
    /**
     * 公司名称
     */
    private String name;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 账套ID
     */
    private Long accountSetsId;

    /**
     * 当前账期
     */
    private String currentAccountDate;

    /**
     * 启用账期
     */
    private String enableDate;
}
