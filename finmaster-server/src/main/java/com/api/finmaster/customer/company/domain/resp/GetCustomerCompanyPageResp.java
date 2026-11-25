package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

/**
 * @Author zhb
 * @Date 2025-06-07
 * @Description
 */
@Data
public class GetCustomerCompanyPageResp implements Serializable {
    private static final long serialVersionUID = -7411491443116637782L;

    /** 主键ID */
    private Long id;

    /** 企业微信openid */
    private String openid;

    /** 公司名称 */
    private String companyName;

    /** 税号 */
    private String taxNumber;

    /** 法人名 */
    private String legalName;

    /** 法人身份证号 */
    private String legalIdCard;

    /** 注册地址（企查查 address） */
    private String registeredAddress;

    /** 税局所在地 */
    private String taxDistrict;

    /** 省份 */
    private String province;

    /** 创建时间 */
    private Date createTime;

    /** 是否启用 0-未启用 1-启用*/
    private Integer enabled;

    /** 是否验证税密 0-未验证 1-已验证 */
    private Integer isVerifyTax;

    /** 客户姓名 */
    private String customerName;

    /** 手机号 */
    private String phone;

    /**
     * 公司类型
     */
    private String companyTypeName;


    /**
     * 公司类型
     */
    private String companyType;

    /**
     * 是否开通发票服务 0-未验证 1-已验证
     */
    private Integer isInvoicing;

    /** 公司状态 COOPERATING-合作中 CANCELLED-合作取消 DEREGISTERED-注销 */
    private String companyStatus;

    /** 工商状态：在业/存续/吊销/注销（企查查 status） */
    private String regStatus;
}
