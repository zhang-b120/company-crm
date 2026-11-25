package com.qykj.finmaster.analytics.overview.controller;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.service.AnalyticsScopeResolver;
import com.qykj.finmaster.analytics.overview.service.OverviewService;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据中心 · 综合看板
 */
@RestController
@RequestMapping("/analytics/overview")
public class OverviewController extends BaseController {

    @Autowired
    private OverviewService overviewService;

    @Autowired
    private AnalyticsScopeResolver scopeResolver;

    /**
     * 综合看板聚合数据
     */
    @GetMapping
    public AjaxResult overview(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(overviewService.getOverview(getLoginUser(), req));
    }

    /**
     * 当前账号可用层级列表
     * 前端层级切换器用它初始化
     */
    @GetMapping("/scopes")
    public AjaxResult availableScopes() {
        return AjaxResult.success(scopeResolver.availableScopes(getLoginUser()));
    }
}
