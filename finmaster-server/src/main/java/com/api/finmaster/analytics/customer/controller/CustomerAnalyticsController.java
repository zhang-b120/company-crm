package com.qykj.finmaster.analytics.customer.controller;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.customer.dto.CustomerAnalyticsResp;
import com.qykj.finmaster.analytics.customer.service.CustomerAnalyticsService;
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

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/analytics/customer")
public class CustomerAnalyticsController extends BaseController {

    @Autowired
    private CustomerAnalyticsService service;

    /**
     * 私海健康度
     */
    @GetMapping("/private-sea")
    public AjaxResult privateSea(@ModelAttribute AnalyticsQueryReq req,
                                 @RequestParam(required = false) Integer dormantDays) {
        return AjaxResult.success(service.privateSea(getLoginUser(), req, dormantDays));
    }

    /**
     * 客户生命周期
     */
    @GetMapping("/lifecycle")
    public AjaxResult lifecycle(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.lifecycle(getLoginUser(), req));
    }

    @GetMapping("/export/private-sea")
    public void exportPrivateSea(@ModelAttribute AnalyticsQueryReq req,
                                 @RequestParam(required = false) Integer dormantDays,
                                 HttpServletResponse response) {
        CustomerAnalyticsResp.PrivateSeaResp resp = service.privateSea(getLoginUser(), req, dormantDays);
        List<CustomerAnalyticsResp.OwnerLoad> list = resp.getOwnerLoad() == null
                ? Collections.emptyList() : resp.getOwnerLoad();
        new ExcelUtil<>(CustomerAnalyticsResp.OwnerLoad.class).exportExcel(response, list, "私海客户健康度");
    }

    /** 公海回收分析 */
    @GetMapping("/public-sea")
    public AjaxResult publicSea(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.publicSea(getLoginUser(), req));
    }
}
