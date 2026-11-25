package com.qykj.finmaster.customer.company.domain.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 小程序保存公司税务凭证请求
 */
@Data
public class UaSaveCompanyTaxCredentialReq implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 微信 openId（身份校验用） */
    @NotBlank(message = "openId 不能为空")
    private String openId;

    /** 公司ID */
    @NotNull(message = "companyId 不能为空")
    private Long companyId;

    /** 个税密码 */
    private String individualTaxPassword;

    /** 个税 App 状态：1-已下载 2-已提醒下载 null-未填写 */
    private Integer individualTaxAppDownloaded;

    /** 电子税务局密码 */
    @JsonProperty("eTaxPassword")
    private String eTaxPassword;

    /** 电子税务局 App 状态：1-已下载 2-已提醒下载 null-未填写 */
    @JsonProperty("eTaxAppDownloaded")
    private Integer eTaxAppDownloaded;

    /** 办税员是否配置 0-否 1-是 */
    private Integer taxOfficerConfigured;

    /** 代理通道是否配置 0-否 1-是 */
    private Integer agentChannelConfigured;
}
