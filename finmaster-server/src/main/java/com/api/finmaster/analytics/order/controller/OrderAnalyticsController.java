package com.qykj.finmaster.analytics.order.controller;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.order.dto.OrderAnalyticsResp;
import com.qykj.finmaster.analytics.order.service.OrderAnalyticsService;
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
@RequestMapping("/analytics/order")
public class OrderAnalyticsController extends BaseController {

    @Autowired
    private OrderAnalyticsService service;

    @GetMapping("/summary")
    public AjaxResult summary(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.summary(getLoginUser(), req));
    }

    @GetMapping("/refund")
    public AjaxResult refund(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.refund(getLoginUser(), req));
    }

    /**
     * 订单来源归因：按线索来源聚合订单金额
     */
    @GetMapping("/source-attribution")
    public AjaxResult sourceAttribution(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.sourceAttribution(getLoginUser(), req));
    }

    @GetMapping("/export/sales-attribution")
    public void exportSalesAttribution(@ModelAttribute AnalyticsQueryReq req, HttpServletResponse response) {
        OrderAnalyticsResp.SummaryResp resp = service.summary(getLoginUser(), req);
        List<OrderAnalyticsResp.SalesAttribution> list = resp.getSalesAttribution() == null
                ? Collections.emptyList() : resp.getSalesAttribution();
        new ExcelUtil<>(OrderAnalyticsResp.SalesAttribution.class).exportExcel(response, list, "订单销售归因");
    }

    @GetMapping("/export/refund-owner")
    public void exportRefundOwner(@ModelAttribute AnalyticsQueryReq req, HttpServletResponse response) {
        OrderAnalyticsResp.RefundResp resp = service.refund(getLoginUser(), req);
        List<OrderAnalyticsResp.RefundOwnerItem> list = resp.getRefundByOwner() == null
                ? Collections.emptyList() : resp.getRefundByOwner();
        new ExcelUtil<>(OrderAnalyticsResp.RefundOwnerItem.class).exportExcel(response, list, "退款责任人排行");
    }
}
