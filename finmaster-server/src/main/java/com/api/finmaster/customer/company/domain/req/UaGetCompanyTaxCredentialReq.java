package com.qykj.finmaster.customer.company.domain.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 小程序读取公司税务凭证请求
 */
@Data
public class UaGetCompanyTaxCredentialReq implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 微信 openId（身份校验用） */
    @NotBlank(message = "openId 不能为空")
    private String openId;

    /** 公司ID */
    @NotNull(message = "companyId 不能为空")
    private Long companyId;
}
