package com.qykj.finmaster.customer.company.domain.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-08-25
 * @Description
 */
@Data
public class GetCompanyServiceCompanyInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -7133835873058129473L;

    private Long id;

    /** 公司名称 */
    private String companyName;

    /** 税号 */
    private String taxNumber;

    /** 法人名 */
    private String legalName;

    /** 法人身份证号 */
    private String legalIdCard;

    /** 法人手机号 */
    private String legalPhone;

    /** 是否启用 0-未启用 1-启用*/
    private Integer enabled;

    /** 是否验证税密 0-未验证 1-已验证 */
    private Integer isVerifyTax;

    /** 企业类型 字典COMPANY_TYPE */
    private String companyType;

    /** 企业类型 字典COMPANY_TYPE */
    private String companyTypeName;

    /** 是否开通发票服务 0-未开通 1-已开通 */
    private Integer isInvoicing;

    /** 公司状态 COOPERATING-合作中 CANCELLED-合作取消 DEREGISTERED-注销 */
    private String companyStatus;

    /** 状态说明 */
    private String statusRemark;

    /** 是否记账户 0-否 1-是 */
    private Integer isBookkeeping;

    /** 是否个体工商户 0-否 1-是 */
    private Integer isIndividual;

    /** 双免个体 0-否 1-是 */
    private Integer isDoubleExemption;

    /** 成立日期（企查查 startDate） */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate establishedDate;

    /** 工商状态：在业/存续/吊销/注销（企查查 status） */
    private String regStatus;

    /** 注册地址（企查查 address） */
    private String registeredAddress;

    /** 税局所在地（31 省份或厦门/深圳/青岛/宁波/大连 5 个计划单列市） */
    private String taxDistrict;

    /** 省份（公司注册地省份，区别于税局所在地） */
    private String province;
}