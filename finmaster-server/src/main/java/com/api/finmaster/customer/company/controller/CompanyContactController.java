package com.qykj.finmaster.customer.company.controller;

import com.qykj.finmaster.common.annotation.Log;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.enums.BusinessType;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.customer.company.domain.req.AddCompanyContactReq;
import com.qykj.finmaster.customer.company.domain.req.UpdateCompanyContactReq;
import com.qykj.finmaster.customer.company.service.CompanyContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 公司联系人 Controller
 */
@RestController
@RequestMapping("/company/contact")
public class CompanyContactController extends BaseController {

    @Autowired
    private CompanyContactService contactService;

    /** 根据公司ID查询联系人列表 */
    @PreAuthorize("@ss.hasPermi('customer:company:query')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Long companyId) {
        return AjaxResult.success(contactService.listByCompanyId(companyId));
    }

    /** 新增公司联系人 */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "公司联系人", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody AddCompanyContactReq req) {
        return AjaxResult.success(contactService.add(req, getLoginUser()));
    }

    /** 修改公司联系人 */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "公司联系人", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody UpdateCompanyContactReq req) {
        return toAjax(contactService.update(req, getLoginUser()));
    }

    /** 删除公司联系人 */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "公司联系人", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(contactService.delete(id));
    }
}
