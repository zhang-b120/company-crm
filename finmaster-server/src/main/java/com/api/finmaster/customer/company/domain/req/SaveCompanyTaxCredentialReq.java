package com.qykj.finmaster.customer.company.domain.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class SaveCompanyTaxCredentialReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "公司ID不能为空")
    private Long companyId;

    /** 个税密码 */
    private String individualTaxPassword;

    /** 个税 App 状态 1-已下载 2-已提醒下载 NULL-未填写 */
    private Integer individualTaxAppDownloaded;

    /** 电子税务局密码 */
    @com.fasterxml.jackson.annotation.JsonProperty("eTaxPassword")
    private String eTaxPassword;

    /** 电子税务局 App 状态 1-已下载 2-已提醒下载 NULL-未填写 */
    @com.fasterxml.jackson.annotation.JsonProperty("eTaxAppDownloaded")
    private Integer eTaxAppDownloaded;

    /** 办税员是否配置 0-否 1-是 */
    private Integer taxOfficerConfigured;

    /** 代理通道是否配置 0-否 1-是 */
    private Integer agentChannelConfigured;
}
