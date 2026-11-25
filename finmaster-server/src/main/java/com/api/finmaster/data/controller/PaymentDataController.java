package com.qykj.finmaster.data.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.data.domain.req.GetPaymentDataDetailPageReq;
import com.qykj.finmaster.data.domain.req.GetPaymentDataReq;
import com.qykj.finmaster.data.service.OrderOverviewServiceImpl;
import com.qykj.finmaster.data.service.PaymentDataServiceImpl;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/paymentData")
public class PaymentDataController extends BaseController {

    @Autowired
    private PaymentDataServiceImpl paymentDataService;

    @Autowired
    private OrderOverviewServiceImpl orderOverviewService;

    @PostMapping("/list")
    public AjaxResult list(@RequestBody GetPaymentDataReq req) {
        return AjaxResult.success(paymentDataService.list(req));
    }

    /**
     * 获取支付数据明细
     * @param req
     * @return
     */
    @PermitAll
    @PostMapping("/paymentDataDetailPage")
    public TableDataInfo paymentDataDetailPage(@RequestBody GetPaymentDataDetailPageReq req) {
        return getDataTable(paymentDataService.getPaymentDataDetailData(req));
    }

    /**
     * 导出支付数据明细
     * @param req
     * @param response
     * @return
     */
    @PostMapping("/exportPaymentDataDetailPage")
    public void exportPaymentDataDetailPage(@RequestBody GetPaymentDataDetailPageReq req, HttpServletResponse response) {
        paymentDataService.exportPaymentDataDetailPage(req, response);
    }

    /**
     * 数据总览（当日/当月/近7天/近12月 订单成交与退款统计）
     */
    @GetMapping("/orderOverview")
    public AjaxResult orderOverview() {
        return AjaxResult.success(orderOverviewService.getOverview());
    }
}
