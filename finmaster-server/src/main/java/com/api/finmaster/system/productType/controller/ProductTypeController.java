package com.qykj.finmaster.system.productType.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;

import com.qykj.finmaster.system.productType.domain.ProductType;
import com.qykj.finmaster.system.productType.domain.req.AddProductTypeReq;
import com.qykj.finmaster.system.productType.domain.req.UpdateProductTypeReq;
import com.qykj.finmaster.system.productType.domain.resp.GetProductTypeResp;
import com.qykj.finmaster.system.productType.service.ProductTypeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 产品类型Controller
 */
@RestController
@RequestMapping("/system/productType")
public class ProductTypeController extends BaseController {
    @Autowired
    private ProductTypeServiceImpl productTypeService;

    /**
     * 获取产品类型列表
     */
    @GetMapping("/list")
    public AjaxResult list() {
        // 不限制租户，获取所有产品类型
        List<GetProductTypeResp> list = productTypeService.selectProductTypeList();
        return AjaxResult.success(list);
    }

    /**
     * 获取产品类型详细信息
     */
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(productTypeService.selectProductTypeById(id));
    }

    /**
     * 新增产品类型
     */
    @PostMapping
    public AjaxResult add(@RequestBody AddProductTypeReq req) {
        return toAjax(productTypeService.insertProductType(req, getTenantId(), getLoginUser()));
    }

    /**
     * 修改产品类型
     */
    @PutMapping
    public AjaxResult edit(@RequestBody UpdateProductTypeReq req) {
        return toAjax(productTypeService.updateProductType(req, getTenantId(), getLoginUser()));
    }

    /**
     * 删除产品类型
     */
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(productTypeService.deleteProductTypeById(id));
    }

    /**
     * 搜索产品类型
     */
    @GetMapping("/search")
    public AjaxResult search(@RequestParam(required = false) String keyword) {
        List<ProductType> list = productTypeService.searchProductTypes(keyword);
        return AjaxResult.success(list);
    }
}
