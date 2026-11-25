package com.qykj.finmaster.oa.salary.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 社保公积金配置 oa_social_insurance_config
 */
@Data
@TableName("oa_social_insurance_config")
public class OaSocialInsuranceConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("real_name")
    private String realName;

    /** 社保缴费基数（为空则用基本工资） */
    @TableField("insurance_base")
    private BigDecimal insuranceBase;

    @TableField("endowment_personal_rate")
    private BigDecimal endowmentPersonalRate;

    @TableField("endowment_company_rate")
    private BigDecimal endowmentCompanyRate;

    @TableField("medical_personal_rate")
    private BigDecimal medicalPersonalRate;

    @TableField("medical_company_rate")
    private BigDecimal medicalCompanyRate;

    @TableField("unemployment_personal_rate")
    private BigDecimal unemploymentPersonalRate;

    @TableField("unemployment_company_rate")
    private BigDecimal unemploymentCompanyRate;

    @TableField("housing_fund_personal_rate")
    private BigDecimal housingFundPersonalRate;

    @TableField("housing_fund_company_rate")
    private BigDecimal housingFundCompanyRate;

    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
