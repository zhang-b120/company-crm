package com.qykj.finmaster.analytics.cs.controller;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.cs.dto.CsAnalyticsResp;
import com.qykj.finmaster.analytics.cs.service.CsAnalyticsService;
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
@RequestMapping("/analytics/cs")
public class CsAnalyticsController extends BaseController {

    @Autowired
    private CsAnalyticsService service;

    @GetMapping("/renewal")
    public AjaxResult renewal(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.renewal(getLoginUser(), req));
    }

    @GetMapping("/task-summary")
    public AjaxResult taskSummary(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.taskSummary(getLoginUser(), req));
    }

    @GetMapping("/customer-profile")
    public AjaxResult customerProfile(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.customerProfile(getLoginUser(), req));
    }

    /** 未续费原因聚合 */
    @GetMapping("/not-renew-reasons")
    public AjaxResult notRenewReasons(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.notRenewReasons(getLoginUser(), req));
    }

    @GetMapping("/export/butler-ranking")
    public void exportButlerRanking(@ModelAttribute AnalyticsQueryReq req, HttpServletResponse response) {
        CsAnalyticsResp.RenewalResp resp = service.renewal(getLoginUser(), req);
        List<CsAnalyticsResp.ButlerRankItem> list = resp.getButlerRanking() == null
                ? Collections.emptyList() : resp.getButlerRanking();
        new ExcelUtil<>(CsAnalyticsResp.ButlerRankItem.class).exportExcel(response, list, "管家续费排行");
    }

    @GetMapping("/export/task-owner")
    public void exportTaskOwner(@ModelAttribute AnalyticsQueryReq req, HttpServletResponse response) {
        CsAnalyticsResp.TaskSummaryResp resp = service.taskSummary(getLoginUser(), req);
        List<CsAnalyticsResp.TaskOwnerRank> list = resp.getOwnerRanking() == null
                ? Collections.emptyList() : resp.getOwnerRanking();
        new ExcelUtil<>(CsAnalyticsResp.TaskOwnerRank.class).exportExcel(response, list, "CS任务处理人排行");
    }
}
