package com.qykj.finmaster.data.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.data.service.BossAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data/boss")
public class BossAnalyticsController extends BaseController {

    @Autowired
    private BossAnalyticsService bossAnalyticsService;

    /**
     * 老板视角数据概览
     */
    @GetMapping("/analytics")
    public AjaxResult analytics() {
        Long tenantId = getTenantId();
        return AjaxResult.success(bossAnalyticsService.getAnalytics(tenantId));
    }
}
