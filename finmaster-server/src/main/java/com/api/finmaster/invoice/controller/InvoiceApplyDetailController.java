package com.qykj.finmaster.invoice.controller;

import com.qykj.finmaster.invoice.service.InvoiceApplyDetailServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发票申请明细Controller
 */
@RestController
@RequestMapping("/invoice/apply/detail")
public class InvoiceApplyDetailController {

    @Autowired
    private InvoiceApplyDetailServiceImpl invoiceApplyDetailService;

} 