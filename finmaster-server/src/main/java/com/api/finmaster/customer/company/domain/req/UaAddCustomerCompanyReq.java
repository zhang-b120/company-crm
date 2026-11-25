package com.qykj.finmaster.customer.company.domain.req;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@Data
public class UaAddCustomerCompanyReq implements Serializable {
    private static final long serialVersionUID = 8953752659542756901L;

    /**
     * 企业微信openid
     */
    private String openid;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     *  税号
     */
    private String taxNumber;

    /**
     * 法人名
     */
    private String legalName;

    /**
     * 企业类型 字典COMPANY_TYPE
     */
    private String companyType;
}