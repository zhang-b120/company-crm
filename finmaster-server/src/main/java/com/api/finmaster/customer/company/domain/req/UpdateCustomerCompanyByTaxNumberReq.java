package com.qykj.finmaster.customer.company.domain.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 按税号更新公司基本信息
 * 用于已存在公司在新增之后再编辑（PC 端 / 小程序通用，小程序版另有 openId 做归属校验）
 */
@Data
public class UpdateCustomerCompanyByTaxNumberReq implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 公司税号（必传，作为定位条件） */
    @NotBlank(message = "公司税号不能为空")
    private String taxNumber;

    /* ==== 以下字段都是按需更新，传 null 表示不改 ==== */
    private String companyName;
    private String companyType;
    private String legalName;
    private String legalIdCard;
    private String legalPhone;
    private LocalDate establishedDate;
    private String regStatus;
    private String registeredAddress;
    private String taxDistrict;
    /** 省份（公司注册地省份，不传 / null 时由后端按地址自动识别） */
    private String province;
    private Integer isInvoicing;
    private Integer isIndividual;
    private String companyStatus;
    private String statusRemark;
}
