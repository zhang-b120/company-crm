package com.qykj.finmaster.invoice.controller;

import com.qykj.finmaster.invoice.service.InvoiceTitleServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发票抬头信息Controller
 */
@RestController
@RequestMapping("/invoice/title")
public class InvoiceTitleController {

    @Autowired
    private InvoiceTitleServiceImpl invoiceTitleService;

} 