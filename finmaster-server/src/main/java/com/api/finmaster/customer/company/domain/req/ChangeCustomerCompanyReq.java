package com.qykj.finmaster.customer.company.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-04
 * @Description
 */
@Data
public class ChangeCustomerCompanyReq implements Serializable {

    private static final long serialVersionUID = 4880963320629627067L;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 微信openID
     */
    private String openId;
}