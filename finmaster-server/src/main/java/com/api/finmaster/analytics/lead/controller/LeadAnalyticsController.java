package com.qykj.finmaster.analytics.lead.controller;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.lead.dto.LeadAnalyticsResp;
import com.qykj.finmaster.analytics.lead.service.LeadAnalyticsService;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics/lead")
public class LeadAnalyticsController extends BaseController {

    @Autowired
    private LeadAnalyticsService service;

    @GetMapping("/funnel")
    public AjaxResult funnel(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.funnel(getLoginUser(), req));
    }

    @GetMapping("/source-roi")
    public AjaxResult sourceRoi(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.sourceRoi(getLoginUser(), req));
    }

    @GetMapping("/follow-up-timing")
    public AjaxResult followUpTiming(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.followUpTiming(getLoginUser(), req));
    }

    @GetMapping("/trend")
    public AjaxResult trend(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.trend(getLoginUser(), req));
    }

    @GetMapping("/export/source-roi")
    public void exportSourceRoi(@ModelAttribute AnalyticsQueryReq req, HttpServletResponse response) {
        List<LeadAnalyticsResp.SourceRoiItem> list = service.sourceRoi(getLoginUser(), req);
        new ExcelUtil<>(LeadAnalyticsResp.SourceRoiItem.class).exportExcel(response, list, "线索来源ROI");
    }
}
