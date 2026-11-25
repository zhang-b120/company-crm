package com.qykj.finmaster.customer.company.domain.req;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-07-29
 * @Description
 */
@Data
public class AddCustomerCompanyReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 6381687425301641703L;

    /** 主键ID */
    private Long id;

    /** 客户ID */
    private Long customerId;

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

    /** 企业类型 字典COMPANY_TYPE */
    private String companyType;

    /** 成立日期（企查查 startDate，格式 yyyy-MM-dd） */
    private LocalDate establishedDate;

    /** 工商状态：在业/存续/吊销/注销（企查查 status） */
    private String regStatus;

    /** 注册地址（企查查 address） */
    private String registeredAddress;

    /** 税局所在地（31 省份或厦门/深圳/青岛/宁波/大连 5 个计划单列市） */
    private String taxDistrict;

}