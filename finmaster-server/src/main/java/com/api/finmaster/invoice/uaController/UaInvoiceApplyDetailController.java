package com.qykj.finmaster.invoice.uaController;

import com.qykj.finmaster.invoice.service.InvoiceApplyDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发票申请明细Controller
 */
@RestController
@RequestMapping("/ua/invoice/apply/detail")
public class UaInvoiceApplyDetailController {

    @Autowired
    private InvoiceApplyDetailServiceImpl invoiceApplyDetailService;

} 