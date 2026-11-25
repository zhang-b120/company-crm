package com.qykj.finmaster.customer.company.domain.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 小程序新增公司请求（对齐 PC 端 AddCustomerCompanyReq，多一个 openId 字段用于定位客户）
 */
@Data
public class UaAddCompanyReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 微信 openId（必填，用于关联客户） */
    @NotBlank(message = "openId 不能为空")
    private String openId;

    /** 公司名称 */
    @NotBlank(message = "公司名称不能为空")
    private String companyName;

    /** 统一社会信用代码 / 税号 */
    @NotBlank(message = "税号不能为空")
    private String taxNumber;

    /** 法人姓名 */
    private String legalName;

    /** 法人身份证号 */
    private String legalIdCard;

    /** 法人手机号 */
    private String legalPhone;

    /** 纳税人类型 COMPANY_TYPE 字典 small-scale-tax / general-tax */
    private String companyType;

    /** 成立日期 yyyy-MM-dd */
    private LocalDate establishedDate;

    /** 工商状态：在业 / 存续 / 吊销 / 注销 */
    private String regStatus;

    /** 注册地址 */
    private String registeredAddress;

    /** 税局所在地 */
    private String taxDistrict;
}
