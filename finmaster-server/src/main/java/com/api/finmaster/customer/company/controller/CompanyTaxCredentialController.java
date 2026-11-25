package com.qykj.finmaster.customer.company.controller;

import com.qykj.finmaster.common.annotation.Log;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.enums.BusinessType;
import com.qykj.finmaster.customer.company.domain.req.SaveCompanyTaxCredentialReq;
import com.qykj.finmaster.customer.company.service.CompanyTaxCredentialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 公司税务凭证 Controller
 */
@RestController
@RequestMapping("/company/tax-credential")
public class CompanyTaxCredentialController extends BaseController {

    @Autowired
    private CompanyTaxCredentialService taxCredentialService;

    /** 根据公司ID查询税务凭证 */
    @PreAuthorize("@ss.hasPermi('customer:company:query')")
    @GetMapping
    public AjaxResult get(@RequestParam Long companyId) {
        return AjaxResult.success(taxCredentialService.getByCompanyId(companyId));
    }

    /** 保存税务凭证（upsert：有则更新，无则新增） */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "公司税务凭证", businessType = BusinessType.UPDATE)
    @PostMapping("/save")
    public AjaxResult save(@Validated @RequestBody SaveCompanyTaxCredentialReq req) {
        return AjaxResult.success(taxCredentialService.save(req, getLoginUser()));
    }
}
