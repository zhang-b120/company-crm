package com.qykj.finmaster.customer.company.domain.resp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-07-29
 * @Description
 */
@Data
public class GetCustomerCompanyInfoResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 5488936609396721872L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 客户ID */
    private Long customerId;

    /** 企业微信openid */
    private String openid;

    /** 公司名称 */
    private String companyName;

    /** 税号 */
    private String taxNumber;

    /** 法人名 */
    private String legalName;

    /** 是否启用 0-未启用 1-启用*/
    private Integer enabled;

    /** 是否验证税密 0-未验证 1-已验证 */
    private Integer isVerifyTax;

    /** 企业类型 字典COMPANY_TYPE */
    private String companyType;

    /** 体验账套ID */
    private Long experienceAccountSetId;

    /** 是否开通发票服务 0-未开通 1-已开通 */
    private Integer isInvoicing;
}