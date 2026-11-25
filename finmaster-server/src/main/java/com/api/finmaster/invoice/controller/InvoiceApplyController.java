package com.qykj.finmaster.invoice.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.invoice.domain.req.GetInvoiceApplyPageListReq;
import com.qykj.finmaster.invoice.domain.req.UpdateApplyInvoiceStatusReq;
import com.qykj.finmaster.invoice.service.InvoiceApplyServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发票申请信息Controller
 */
@RestController
@RequestMapping("/invoice/apply")
public class InvoiceApplyController extends BaseController {

    @Autowired
    private InvoiceApplyServiceImpl invoiceApplyService;

    @PreAuthorize("@ss.hasPermi('customer:invoice:list')")
    @PostMapping("/getInvoiceApplyPageList")
    public TableDataInfo getInvoiceApplyPageList(@RequestBody GetInvoiceApplyPageListReq req){
        return getDataTable(invoiceApplyService.getInvoiceApplyPageList(req));
    }

    /**
     * 修改该状态
     * @param req
     * @return
     */
    @PreAuthorize("@ss.hasPermi('customer:invoice:editStatus')")
    @PostMapping("/updateInvoiceStatus")
    public AjaxResult updateInvoiceStatus(@RequestBody UpdateApplyInvoiceStatusReq req){
        return AjaxResult.success(invoiceApplyService.updateInvoiceStatus(req));
    }
}