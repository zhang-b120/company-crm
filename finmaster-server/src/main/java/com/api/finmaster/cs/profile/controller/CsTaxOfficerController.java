package com.qykj.finmaster.cs.profile.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.cs.profile.domain.req.SaveTaxOfficerReq;
import com.qykj.finmaster.cs.profile.service.CsTaxOfficerServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cs/tax-officer")
public class CsTaxOfficerController extends BaseController {

    @Autowired
    private CsTaxOfficerServiceImpl taxOfficerService;

    /** 查询公司办税员列表 */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Long companyId) {
        return AjaxResult.success(taxOfficerService.listByCompanyId(companyId));
    }

    /** 新增/编辑办税员 */
    @PostMapping("/save")
    public AjaxResult save(@RequestBody SaveTaxOfficerReq req) {
        return toAjax(taxOfficerService.saveTaxOfficer(req));
    }

    /** 删除办税员 */
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(taxOfficerService.deleteTaxOfficer(id));
    }
}
