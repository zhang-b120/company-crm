package com.qykj.finmaster.system.department.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.system.department.entity.req.AddDepartmentReq;
import com.qykj.finmaster.system.department.entity.req.UpdateDepartmentReq;
import com.qykj.finmaster.system.department.service.DepartmentServiceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/system/department")
public class DepartmentController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentController.class);

    @Resource
    private DepartmentServiceImpl departmentService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:department:query')")
    public AjaxResult list() {
        return AjaxResult.success(departmentService.selectDepartmentList());
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:department:add')")
    public AjaxResult add(@RequestBody AddDepartmentReq req) {
        Long tenantId = getTenantId();
        return AjaxResult.success(departmentService.addDepartment(req, tenantId, getLoginUser()));
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:department:edit')")
    public AjaxResult update(@RequestBody UpdateDepartmentReq req) {
        Long tenantId = getTenantId();
        return AjaxResult.success(departmentService.updateDepartment(req, tenantId, getLoginUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:department:delete')")
    public AjaxResult delete(@PathVariable Long id) {
        return AjaxResult.success(departmentService.deleteDepartment(id, getLoginUser()));
    }
}
