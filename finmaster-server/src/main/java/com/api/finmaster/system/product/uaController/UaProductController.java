package com.qykj.finmaster.system.product.uaController;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.system.product.domain.req.UaGetProductListReq;
import com.qykj.finmaster.system.product.service.ProductServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author zhb
 * @Date 2025-06-19
 * @Description
 */
@RestController
@RequestMapping("/ua/system/product")
public class UaProductController extends BaseController {

    @Autowired
    private ProductServiceImpl productService;

    /**
     * 小程序获取产品列表
     * @param req
     * @return
     */
    @PermitAll
    @PostMapping("/uaGetProductList")
    public AjaxResult uaGetProductList(@RequestBody UaGetProductListReq req){
        return AjaxResult.success(productService.uaGetProductList(req));
    }
}
