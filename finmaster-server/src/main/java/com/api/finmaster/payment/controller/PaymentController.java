package com.qykj.finmaster.payment.controller;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.payment.domain.req.*;
import com.qykj.finmaster.payment.service.PaymentServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-06-17
 * @Description
 */
@RestController
@RequestMapping("/ua/pay")
public class PaymentController {

    @Autowired
    private PaymentServiceImpl paymentService;

    @PostMapping("/wxJsapiPrepay")
    @PermitAll
    public AjaxResult wxJsapiPrepay(@RequestBody JsapiPrepayReq req){
        return AjaxResult.success(paymentService.wxJsapiPrepay(req));
    }

    @PostMapping("/wxJsapiQuery")
    public AjaxResult wxJsapiQuery(@RequestBody JsapiQueryReq req){
        return AjaxResult.success(paymentService.wxJsapiQuery(req));
    }

    @PostMapping("/wxJsapiNotify")
    public AjaxResult wxJsapiNotify(@RequestBody Map<String, Object> req){
        return AjaxResult.success(paymentService.wxJsapiNotify(req));
    }

    @PostMapping("/wxJsapiClose")
    public AjaxResult wxJsapiClose(@RequestBody JsapiCloseReq req){
        return AjaxResult.success(paymentService.wxJsapiClose(req));
    }

    @PostMapping("/wxJsapiRefund")
    public AjaxResult wxJsapiRefund(@RequestBody JsapiRefundReq req){
        return AjaxResult.success(paymentService.wxJsapiRefund(req));
    }

}