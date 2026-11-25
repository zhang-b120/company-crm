package com.qykj.finmaster.system.tenant.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.system.tenant.entity.req.AddTenantReq;
import com.qykj.finmaster.system.tenant.entity.req.GetTenantPageReq;
import com.qykj.finmaster.system.tenant.entity.req.UpdateTenantReq;
import com.qykj.finmaster.system.tenant.service.TenantServiceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

/**
 * 租户Controller
 */
@RestController
@RequestMapping("/system/tenant")
public class TenantController extends BaseController {

    @Resource
    private TenantServiceImpl tenantService;

    /**
     * 查询租户列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:tenant:query')")
    public TableDataInfo list(GetTenantPageReq req) {
        return getDataTable(tenantService.selectTenantPage(req));
    }

    /**
     * 获取租户详细信息
     */
    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:tenant:query')")
    public AjaxResult getInfo(@PathVariable Long id) {
        return AjaxResult.success(tenantService.getInfo(id));
    }

    /**
     * 新增租户
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:tenant:add')")
    public AjaxResult add(@Valid @RequestBody AddTenantReq req) {
        return toAjax(tenantService.addTenant(req, getLoginUser()));
    }

    /**
     * 修改租户
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:tenant:edit')")
    public AjaxResult update(@Valid @RequestBody UpdateTenantReq req) {

        return toAjax(tenantService.updateTenant(req, getLoginUser()));
    }

    /**
     * 删除租户
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:tenant:remove')")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(tenantService.deleteTenant(id, getLoginUser()));
    }

    /**
     * 批量删除租户
     */
    @DeleteMapping("/batch")
    @PreAuthorize("@ss.hasPermi('system:tenant:remove')")
    public AjaxResult deleteBatch(@RequestBody Long[] ids) {
        return toAjax(tenantService.deleteTenants(ids, getLoginUser()));
    }

    /**
     * 更新租户状态
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("@ss.hasPermi('system:tenant:edit')")
    public AjaxResult updateStatus(@PathVariable Long id, @RequestBody Integer status) {
        return toAjax(tenantService.updateTenantStatus(id, status, getLoginUser()));
    }

    /**
     * 检查租户编码是否唯一
     */
    @GetMapping("/checkTenantCode")
    public Result<Boolean> checkTenantCode(@RequestParam String tenantCode, @RequestParam(required = false) Long id) {
        boolean isUnique = tenantService.checkTenantCodeUnique(tenantCode, id);
        return Result.ok(isUnique);
    }
}