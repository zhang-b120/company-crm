package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.*;

import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 客户公司信息对象 customer_company
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_company")
public class CustomerCompany extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 客户ID */
    @TableField("customer_id")
    private Long customerId;

    /** 企业微信openid */
    @TableField("open_id")
    private String openid;

    /** 公司名称 */
    @TableField("company_name")
    private String companyName;

    /** 税号 */
    @TableField("tax_number")
    private String taxNumber;

    /** 法人名 */
    @TableField("legal_name")
    private String legalName;

    /** 法人身份证号 */
    @TableField("legal_id_card")
    private String legalIdCard;

    /** 法人证件类型 0身份证 1非身份证（非身份证免身份证二要素校验，工单95/IDV-05，工单99/IDV-09 改名） */
    @TableField("legal_id_type")
    private Integer legalIdType;

    /** 法人手机号 */
    @TableField("legal_phone")
    private String legalPhone;

    /** 成立日期（企查查 startDate） */
    @TableField("established_date")
    private LocalDate establishedDate;

    /** 工商状态：在业/存续/吊销/注销（企查查 status） */
    @TableField("reg_status")
    private String regStatus;

    /** 注册地址（企查查 address） */
    @TableField("registered_address")
    private String registeredAddress;

    /** 税局所在地（31 省份或厦门/深圳/青岛/宁波/大连 5 个计划单列市） */
    @TableField("tax_district")
    private String taxDistrict;

    /** 省份（公司注册地省份，区别于税局所在地） */
    @TableField("province")
    private String province;

    /** 是否启用 0-未启用 1-启用*/
    @TableField("enabled")
    private Integer enabled;

    /** 是否验证税密 0-未验证 1-已验证 */
    @TableField("is_verify_tax")
    private Integer isVerifyTax;

    /** 企业类型 字典COMPANY_TYPE */
    @TableField("company_type")
    private String companyType;

    /** 体验账套ID */
    @TableField("experience_account_set_id")
    private Long experienceAccountSetId;

    /** 是否开通发票服务 0-未开通 1-已开通 */
    @TableField("is_invoicing")
    private Integer isInvoicing;

    /** 是否记账户 0-否 1-是 */
    @TableField("is_bookkeeping")
    private Integer isBookkeeping;

    /** 是否个体工商户 0-否 1-是 */
    @TableField("is_individual")
    private Integer isIndividual;

    /** 双免个体 0-否 1-是（增值税 + 附加税双重减免的个体工商户） */
    @TableField("is_double_exemption")
    private Integer isDoubleExemption;

    /** 公司状态 COOPERATING-合作中 CANCELLED-合作取消 DEREGISTERED-注销 */
    @TableField("company_status")
    private String companyStatus;

    /** 状态说明（合作取消时填写） */
    @TableField("status_remark")
    private String statusRemark;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
} 
