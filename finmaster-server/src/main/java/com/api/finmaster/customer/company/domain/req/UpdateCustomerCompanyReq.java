package com.qykj.finmaster.customer.company.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-06-09
 * @Description
 */
@Data
public class UpdateCustomerCompanyReq implements Serializable {
    private static final long serialVersionUID = 3063706423617713775L;

    /** 公司id */
    private Long id;

    /** 公司名称 */
    private String companyName;

    /** 法人 */
    private String legalName;

    /** 法人身份证号 */
    private String legalIdCard;

    /** 法人手机号 */
    private String legalPhone;

    /**
     * 企业类型
     */
    private String companyType;

    /**
     * 公司税号
     */
    private String taxNumber;

    /**
     * 客户ID
     */
    private Long customerId;

    /** 成立日期（企查查 startDate） */
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