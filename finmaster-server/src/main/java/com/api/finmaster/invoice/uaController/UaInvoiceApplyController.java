package com.qykj.finmaster.invoice.uaController;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.invoice.domain.req.GetInvoiceApplyListReq;
import com.qykj.finmaster.invoice.domain.req.SaveInvoiceApplyReq;
import com.qykj.finmaster.invoice.service.InvoiceApplyServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 发票申请信息Controller
 */
@RestController
@RequestMapping("/ua/invoice/apply")
public class UaInvoiceApplyController extends BaseController {

    @Autowired
    private InvoiceApplyServiceImpl invoiceApplyService;

    @PermitAll
    @PostMapping("/uaSaveInvoiceApply")
    public AjaxResult uaSaveInvoiceApply(@RequestBody SaveInvoiceApplyReq req){
        return invoiceApplyService.saveInvoiceApply(req);
    }

    @PermitAll
    @PostMapping("/uaDeleteInvoiceApply")
    public AjaxResult uaDeleteInvoiceApply(@RequestParam Long id){
        return AjaxResult.success(invoiceApplyService.deleteInvoiceApply(id));
    }

    @PermitAll
    @PostMapping("/uaGetInvoiceApplyList")
    public AjaxResult uaGetInvoiceApplyList(@RequestBody GetInvoiceApplyListReq req){
        return AjaxResult.success(invoiceApplyService.getInvoiceApplyList(req));
    }

} 