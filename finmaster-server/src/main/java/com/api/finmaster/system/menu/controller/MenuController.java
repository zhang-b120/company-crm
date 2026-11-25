package com.qykj.finmaster.system.menu.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.menu.entity.req.AddMenuReq;
import com.qykj.finmaster.system.menu.entity.req.UpdateMenuReq;
import com.qykj.finmaster.system.menu.service.MenuServiceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/system/menu")
public class MenuController extends BaseController {

    @Resource
    private MenuServiceImpl menuService;

    /**
     * 获取当前用户的菜单列表
     */
    @GetMapping("/list")
    public AjaxResult list(Authentication authentication) {
        // 从认证信息中获取用户信息
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        //根据用户身份获取菜单列表
        return AjaxResult.success(menuService.getUserMenus(loginUser.getUserId()));
    }

    /**
     * 获取菜单树
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:query')")
    public AjaxResult tree() {
        return AjaxResult.success(menuService.buildTree(menuService.list()));
    }

    /**
     * 新增菜单
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public AjaxResult add(@Valid @RequestBody AddMenuReq req) {
        return toAjax(menuService.addMenu(req, getTenantId(), getLoginUser()));
    }

    /**
     * 修改菜单
     */
    @PutMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public AjaxResult update(@Valid @RequestBody UpdateMenuReq req) {
        return toAjax(menuService.updateMenu(req, getTenantId(), getLoginUser()));
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:remove')")
    public AjaxResult delete(@PathVariable Long id) {
        return toAjax(menuService.removeMenu(id, getTenantId(), getLoginUser()));
    }
}
