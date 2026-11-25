package com.qykj.finmaster.customer.customerInfo.uaController;

import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.customer.customerInfo.domain.req.UaGetCustomerInfoReq;
import com.qykj.finmaster.customer.customerInfo.domain.resp.UaGetCustomerInfoResp;
import com.qykj.finmaster.customer.customerInfo.service.CustomerInfoServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@RestController
@RequestMapping("/ua/crm/customer")
public class UaCustomerInfoController {

    @Autowired
    private CustomerInfoServiceImpl customerInfoService;

    @PostMapping("/uaGetCustomerInfo")
    @PermitAll
    @Deprecated
    public Result<UaGetCustomerInfoResp> getCustomerInfo(@RequestBody UaGetCustomerInfoReq req) {
        return Result.ok(customerInfoService.getCustomerInfo(req));
    }
}
