package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公司税务凭证收集 company_tax_credential
 * 一公司一行，按需扩充字段，数据仅作收集，不做强校验
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_tax_credential")
public class CompanyTaxCredential extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司ID（唯一） */
    @TableField("company_id")
    private Long companyId;

    /** 个人所得税密码 */
    @TableField("individual_tax_password")
    private String individualTaxPassword;

    /** 个税 App 状态 1-已下载 2-已提醒下载 NULL-未填写 */
    @TableField("individual_tax_app_downloaded")
    private Integer individualTaxAppDownloaded;

    /** 电子税务局密码 */
    @TableField("e_tax_password")
    @JsonProperty("eTaxPassword")
    private String eTaxPassword;

    /** 电子税务局 App 状态 1-已下载 2-已提醒下载 NULL-未填写 */
    @TableField("e_tax_app_downloaded")
    @JsonProperty("eTaxAppDownloaded")
    private Integer eTaxAppDownloaded;

    /** 办税员是否配置 0-否 1-是 */
    @TableField("tax_officer_configured")
    private Integer taxOfficerConfigured;

    /** 代理通道是否配置 0-否 1-是 */
    @TableField("agent_channel_configured")
    private Integer agentChannelConfigured;
}
