package com.qykj.finmaster.analytics.sales.controller;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.sales.dto.SalesPerformanceResp;
import com.qykj.finmaster.analytics.sales.service.SalesAnalyticsService;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/analytics/sales")
public class SalesAnalyticsController extends BaseController {

    @Autowired
    private SalesAnalyticsService service;

    /**
     * 销售业绩聚合（一次返回 KPI + 趋势 + 排行榜 + 部门分布）
     * 按 req.scope 切换返回内容：PERSONAL 只有 KPI+趋势、TEAM 额外附排行、COMPANY 额外附部门分布
     */
    @GetMapping("/performance")
    public AjaxResult performance(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.performance(getLoginUser(), req));
    }

    @GetMapping("/export/ranking")
    public void exportRanking(@ModelAttribute AnalyticsQueryReq req, HttpServletResponse response) {
        SalesPerformanceResp resp = service.performance(getLoginUser(), req);
        List<SalesPerformanceResp.RankItem> list = resp.getRanking() == null
                ? Collections.emptyList() : resp.getRanking();
        new ExcelUtil<>(SalesPerformanceResp.RankItem.class).exportExcel(response, list, "销售业绩排行");
    }
}
