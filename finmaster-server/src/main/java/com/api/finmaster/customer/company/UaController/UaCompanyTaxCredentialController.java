package com.qykj.finmaster.customer.company.UaController;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.customer.company.domain.req.UaGetCompanyTaxCredentialReq;
import com.qykj.finmaster.customer.company.domain.req.UaSaveCompanyTaxCredentialReq;
import com.qykj.finmaster.customer.company.service.CompanyTaxCredentialService;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序：公司税务凭证收集（PC 端对应 /company/tax-credential）
 * 客户在微信小程序中自助填写 4 个字段：
 *   - 个税密码 / 个税App 状态
 *   - 电子税务局密码 / 电子税务局App 状态
 *
 * 权限：以 openId 校验与 companyId 的归属关系（customer_company.openid）
 */
@RestController
@RequestMapping("/ua/company/tax-credential")
public class UaCompanyTaxCredentialController {

    @Autowired
    private CompanyTaxCredentialService service;

    /**
     * 读取某家公司的税务凭证
     */
    @PermitAll
    @PostMapping("/get")
    public AjaxResult get(@RequestBody @Valid UaGetCompanyTaxCredentialReq req) {
        return AjaxResult.success(service.uaGet(req));
    }

    /**
     * 保存/更新税务凭证（upsert，按 companyId 唯一）
     */
    @PermitAll
    @PostMapping("/save")
    public AjaxResult save(@RequestBody @Valid UaSaveCompanyTaxCredentialReq req) {
        return AjaxResult.success(service.uaSave(req));
    }
}
