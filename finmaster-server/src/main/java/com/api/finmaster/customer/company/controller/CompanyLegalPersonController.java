package com.qykj.finmaster.customer.company.controller;

import com.qykj.finmaster.common.annotation.Log;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.enums.BusinessType;
import com.qykj.finmaster.customer.company.domain.req.AddCompanyLegalPersonReq;
import com.qykj.finmaster.customer.company.domain.req.BatchImportStockholderReq;
import com.qykj.finmaster.customer.company.domain.req.UpdateCompanyLegalPersonReq;
import com.qykj.finmaster.customer.company.service.CompanyLegalPersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 公司法人/股东信息 Controller
 */
@RestController
@RequestMapping("/company/legal-person")
public class CompanyLegalPersonController extends BaseController {

    @Autowired
    private CompanyLegalPersonService legalPersonService;

    /** 根据公司ID查询法人列表 */
    @PreAuthorize("@ss.hasPermi('customer:company:query')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Long companyId) {
        return AjaxResult.success(legalPersonService.listByCompanyId(companyId));
    }

    /** 新增法人 */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "公司法人信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AddCompanyLegalPersonReq req) {
        return AjaxResult.success(legalPersonService.add(req, getLoginUser()));
    }

    /** 修改法人 */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "公司法人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody UpdateCompanyLegalPersonReq req) {
        return toAjax(legalPersonService.update(req, getLoginUser()));
    }

    /** 删除法人 */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "公司法人信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(legalPersonService.delete(id));
    }

    /** 从天眼查股东数据批量导入（同名跳过） */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "批量导入股东信息", businessType = BusinessType.INSERT)
    @PostMapping("/batch-import")
    public AjaxResult batchImport(@Validated @RequestBody BatchImportStockholderReq req) {
        int count = legalPersonService.batchImportFromTyc(req.getCompanyId(), req.getStockholders(), getLoginUser());
        return AjaxResult.success("已导入 " + count + " 条股东信息", count);
    }
}
