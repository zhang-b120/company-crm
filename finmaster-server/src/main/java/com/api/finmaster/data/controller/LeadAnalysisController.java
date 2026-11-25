package com.qykj.finmaster.data.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.data.domain.req.GetLeadAnalysisDataReq;
import com.qykj.finmaster.data.domain.req.GetSaleLeadListReq;
import com.qykj.finmaster.data.service.LeadAnalysisServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/leadAnalysisData")
public class LeadAnalysisController extends BaseController {

    @Autowired
    private LeadAnalysisServiceImpl leadAnalysisService;

    /**
     * 获取线索数据
     * @param req
     * @return
     */
    @PostMapping("/list")
    public AjaxResult list(@RequestBody GetLeadAnalysisDataReq req) {
        return AjaxResult.success(leadAnalysisService.list(req));
    }

    /**
     * 导出线束数据
     * @param req
     * @param response
     */
    @PostMapping("/export")
    public void export(@RequestBody GetLeadAnalysisDataReq req, HttpServletResponse response) {
        leadAnalysisService.export(req, response);
    }

    /**
     * 获取线索数据(推广部)
     * @param req
     * @return
     */
    @PostMapping("/promotionList")
    public AjaxResult promotionList(@RequestBody GetLeadAnalysisDataReq req) {
        return AjaxResult.success(leadAnalysisService.promotionList(req));
    }

    /**
     * 导出线索数据(推广部)
     * @param req
     * @return
     */
    @PostMapping("/exportPromotionList")
    public void exportPromotionList(@RequestBody GetLeadAnalysisDataReq req, HttpServletResponse response) {
        leadAnalysisService.exportPromotionList(req, response);
    }

    /**
     * 销售线索数据
     * @param req
     * @return
     */
    @PostMapping("/saleLeadList")
    private AjaxResult saleLeadList(@RequestBody GetSaleLeadListReq req){
        return AjaxResult.success(leadAnalysisService.saleLeadList(req, getTenantId()));
    }

    /**
     * 导出销售线索数据
     * @param req
     * @return
     */
    @PostMapping("/exportSaleLeadList")
    public void exportSaleLeadList(@RequestBody GetSaleLeadListReq req, HttpServletResponse response) {
        leadAnalysisService.exportSaleLeadList(req, response, getTenantId());
    }

    /**
     * 销售订单数据
     * @return
     */
    @PostMapping("/saleOrderList")
    private AjaxResult saleOrderList(){
        return AjaxResult.success(leadAnalysisService.saleOrderList(getTenantId()));
    }

    /**
     * 导出销售订单数据
     * @return
     */
    @PostMapping("/exportSaleOrderList")
    public void exportSaleOrderList(HttpServletResponse response) {
        leadAnalysisService.exportSaleOrderList(response, getTenantId());
    }
}
