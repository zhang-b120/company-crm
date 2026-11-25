package com.qykj.finmaster.analytics.fieldwork.controller;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.fieldwork.dto.FieldWorkAnalyticsResp;
import com.qykj.finmaster.analytics.fieldwork.service.FieldWorkAnalyticsService;
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
@RequestMapping("/analytics/fieldwork")
public class FieldWorkAnalyticsController extends BaseController {

    @Autowired
    private FieldWorkAnalyticsService service;

    @GetMapping("/overview")
    public AjaxResult overview(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.overview(getLoginUser(), req));
    }

    @GetMapping("/ranking")
    public AjaxResult ranking(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.ranking(getLoginUser(), req));
    }

    @GetMapping("/export/ranking")
    public void exportRanking(@ModelAttribute AnalyticsQueryReq req, HttpServletResponse response) {
        List<FieldWorkAnalyticsResp.RankItem> list = service.ranking(getLoginUser(), req);
        new ExcelUtil<>(FieldWorkAnalyticsResp.RankItem.class).exportExcel(response, list, "外勤人员排行");
    }
}
