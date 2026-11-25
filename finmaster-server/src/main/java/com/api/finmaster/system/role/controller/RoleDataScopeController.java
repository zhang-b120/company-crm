package com.qykj.finmaster.system.role.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.system.role.entity.dto.RoleDataScopeDTO;
import com.qykj.finmaster.system.role.service.RoleDataScopeServiceImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/system/role/data-scope")
public class RoleDataScopeController extends BaseController {

    @Autowired
    private RoleDataScopeServiceImpl roleDataScopeService;

    @GetMapping("/{roleId}")
    @PreAuthorize("@ss.hasPermission('system:role:dataScope')")
    public AjaxResult getRoleDataScope(@PathVariable Long roleId) {
        return AjaxResult.success(roleDataScopeService.getRoleDataScope(roleId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermission('system:role:dataScope')")
    public AjaxResult assignRoleDataScope(@Valid @RequestBody RoleDataScopeDTO dto) {
        return AjaxResult.success(roleDataScopeService.saveRoleDataScope(
                dto.getRoleId(), dto.getDataScope(), dto.getDeptIds(), getTenantId(), getLoginUser()));
    }
} 