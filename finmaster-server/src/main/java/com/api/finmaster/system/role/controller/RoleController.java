package com.qykj.finmaster.system.role.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.system.role.entity.req.AddRoleReq;
import com.qykj.finmaster.system.role.entity.req.GetRolePageReq;
import com.qykj.finmaster.system.role.entity.req.UpdateRoleReq;
import com.qykj.finmaster.system.role.service.RoleServiceImpl;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/system/role")
public class RoleController extends BaseController {

    @Resource
    private RoleServiceImpl roleService;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    public TableDataInfo list(GetRolePageReq req) {
        Long tenantId = getTenantId();
        return getDataTable(roleService.selectRoleList(req, tenantId));
    }

    /**
     * 获取可分配的角色列表（用于用户管理中的角色分配）
     * 包含超级管理员角色和当前租户的角色
     */
    @GetMapping("/assignable")
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    public AjaxResult getAssignableRoles() {
        Long tenantId = getTenantId();
        return AjaxResult.success(roleService.getAssignableRoles(tenantId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    public AjaxResult add(@RequestBody AddRoleReq req) {
        Long tenantId = getTenantId();
        return AjaxResult.success(roleService.addRole(req, tenantId, getLoginUser()));
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    public AjaxResult update(@RequestBody UpdateRoleReq req) {
        return AjaxResult.success(roleService.updateRole(req, getLoginUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:role:delete')")
    public AjaxResult delete(@PathVariable Long id) {
        return AjaxResult.success(roleService.removeRoleById(id, getLoginUser()));
    }

    @PostMapping("/{roleId}/menus")
    @PreAuthorize("@ss.hasPermi('system:role:assignMenu')")
    public AjaxResult assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        return toAjax(roleService.assignMenus(roleId, menuIds, getTenantId(), getLoginUser()));
    }

    @GetMapping("/{roleId}/menu")
    @PreAuthorize("@ss.hasPermi('system:role:assignMenu')")
    public AjaxResult getRoleMenus(@PathVariable Long roleId) {
        return AjaxResult.success(roleService.findMenuIdsByRoleId(roleId));
    }

    @PostMapping("/{roleId}/data-scope")
    @PreAuthorize("@ss.hasPermi('system:role:dataScope')")
    public AjaxResult assignDataScope(
            @PathVariable Long roleId,
            @RequestParam Integer dataScope,
            @RequestParam(required = false) String deptIds) {
        return toAjax(roleService.assignDataScope(roleId, dataScope, deptIds, getTenantId(), getLoginUser()));
    }

    @GetMapping("/{roleId}/data-scope")
    @PreAuthorize("@ss.hasPermi('system:role:dataScope')")
    public AjaxResult getRoleDataScope(@PathVariable Long roleId) {
        return AjaxResult.success(roleService.getRoleDataScope(roleId));
    }
}
