package com.qykj.finmaster.customer.company.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@Data
public class GetCustomerCompanyListReq implements Serializable {
    private static final long serialVersionUID = -1084118271214317287L;

    /**
     * 微信openID
     */
    private String openId;
}