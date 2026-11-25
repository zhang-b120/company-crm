package com.qykj.finmaster.system.bizConfig.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.system.bizConfig.entity.SysBizConfig;
import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务模块配置 Controller
 */
@RestController
@RequestMapping("/system/biz-config")
public class SysBizConfigController extends BaseController {

    @Autowired
    private SysBizConfigService service;

    /**
     * 列出所有可见模块名（前端 Tab 用）
     */
    @PreAuthorize("@ss.hasPermi('system:biz-config:list')")
    @GetMapping("/modules")
    public AjaxResult listModules() {
        return AjaxResult.success(service.listModules());
    }

    /**
     * 按 key 取单个配置的当前值（无权限校验，任意登录用户可读取，供消息提醒过滤等公开场景使用）。
     * 注意：用 (String, Object) 重载强制走 data 字段，避免 AjaxResult.success(String) 把值塞到 msg。
     */
    @GetMapping("/value")
    public AjaxResult getByKey(@RequestParam("key") String key) {
        String value = service.getString(key);
        return AjaxResult.success("", (Object) value);
    }

    /**
     * 按模块查配置项（前端右侧卡片渲染用）
     */
    @PreAuthorize("@ss.hasPermi('system:biz-config:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam String module) {
        return AjaxResult.success(service.listByModule(module));
    }

    /**
     * 批量更新当前值（前端"保存修改"一次提交所有变更项）
     * 入参：[{id, configValue}, ...]
     */
    @PreAuthorize("@ss.hasPermi('system:biz-config:edit')")
    @PutMapping("/batch")
    public AjaxResult batchUpdate(@RequestBody List<SysBizConfig> updates) {
        return toAjax(service.batchUpdate(updates, getLoginUser()));
    }
}
