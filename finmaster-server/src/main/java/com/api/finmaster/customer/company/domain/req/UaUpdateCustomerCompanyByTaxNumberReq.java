package com.qykj.finmaster.customer.company.domain.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 小程序按税号更新公司基本信息
 * 通过 openId 校验归属：customer_company.openid = openId 且 tax_number = taxNumber 才允许更新
 */
@Data
public class UaUpdateCustomerCompanyByTaxNumberReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "openId 不能为空")
    private String openId;

    @NotBlank(message = "公司税号不能为空")
    private String taxNumber;

    private String companyName;
    private String companyType;
    private String legalName;
    private String legalIdCard;
    private String legalPhone;
    private LocalDate establishedDate;
    private String regStatus;
    private String registeredAddress;
    private String taxDistrict;
    /** 省份（不传 / null 时由后端按地址自动识别） */
    private String province;
    private Integer isInvoicing;
    private Integer isIndividual;
}
