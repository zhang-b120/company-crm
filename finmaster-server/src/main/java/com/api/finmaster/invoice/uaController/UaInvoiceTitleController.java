package com.qykj.finmaster.invoice.uaController;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.invoice.domain.req.SaveInvoiceTitleReq;
import com.qykj.finmaster.invoice.service.InvoiceTitleServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 发票抬头信息Controller
 */
@RestController
@RequestMapping("/ua/invoice/title")
public class UaInvoiceTitleController {

    @Autowired
    private InvoiceTitleServiceImpl invoiceTitleService;

    /**
     * 保存公司抬头
     * @param req
     * @return
     */
    @PermitAll
    @PostMapping("/uaSaveInvoiceTitle")
    public AjaxResult uaSaveInvoiceTitle(@RequestBody SaveInvoiceTitleReq req){
        return invoiceTitleService.saveInvoiceTitle(req);
    }

    /**
     * 获取公司抬头
     * @param companyId
     * @return
     */
    @PermitAll
    @PostMapping("/uaGetInvoiceTitle")
    public AjaxResult uaGetInvoiceTitle(@RequestParam Long companyId){
        return AjaxResult.success(invoiceTitleService.getInvoiceTitle(companyId));
    }
}