package com.qykj.finmaster.analytics.call.controller;

import com.qykj.finmaster.analytics.call.dto.CallAnalyticsResp;
import com.qykj.finmaster.analytics.call.service.CallAnalyticsService;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analytics/call")
public class CallAnalyticsController extends BaseController {

    @Autowired
    private CallAnalyticsService service;

    @GetMapping("/overview")
    public AjaxResult overview(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.overview(getLoginUser(), req));
    }

    @GetMapping("/ranking")
    public AjaxResult ranking(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.ranking(getLoginUser(), req));
    }

    @GetMapping("/low-score")
    public AjaxResult lowScoreCalls(@ModelAttribute AnalyticsQueryReq req,
                                    @RequestParam(required = false) Integer limit) {
        return AjaxResult.success(service.lowScoreCalls(getLoginUser(), req, limit));
    }

    @GetMapping("/export/ranking")
    public void exportRanking(@ModelAttribute AnalyticsQueryReq req, HttpServletResponse response) {
        List<CallAnalyticsResp.RankingItem> list = service.ranking(getLoginUser(), req);
        new ExcelUtil<>(CallAnalyticsResp.RankingItem.class).exportExcel(response, list, "通话人员排行");
    }
}
