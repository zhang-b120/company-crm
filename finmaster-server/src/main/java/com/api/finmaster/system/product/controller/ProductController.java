package com.qykj.finmaster.system.product.controller;

import java.util.List;

import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.system.product.domain.req.AddProductionReq;
import com.qykj.finmaster.system.product.domain.req.GetProductionPageReq;
import com.qykj.finmaster.system.product.domain.req.UpdateProductionReq;
import com.qykj.finmaster.system.product.domain.resp.ExportProductionResp;
import com.qykj.finmaster.system.product.service.ProductServiceImpl;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.qykj.finmaster.common.annotation.Log;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.enums.BusinessType;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 产品Controller
 */
@RestController
@RequestMapping("/system/product")
public class ProductController extends BaseController {
    @Autowired
    private ProductServiceImpl productService;

    /**
     * 查询产品列表
     */
    @PreAuthorize("@ss.hasPermi('system:product:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetProductionPageReq getProductionPageReq) {
        List<GetProductionPageReq> list = productService.selectProductList(getProductionPageReq);
        return getDataTable(list);
    }

    /**
     * 导出产品列表
     */
    @PreAuthorize("@ss.hasPermi('system:product:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, GetProductionPageReq getProductionPageReq) {
        List<GetProductionPageReq> list = productService.selectProductList(getProductionPageReq);
        List<ExportProductionResp> exportProductionRespList = CopierCopyUtils.convertTo(list, ExportProductionResp.class);
        ExcelUtil<ExportProductionResp> util = new ExcelUtil<>(ExportProductionResp.class);
        util.exportExcel(response, exportProductionRespList, "产品数据");
    }

    /**
     * 获取产品详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:product:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(productService.selectProductById(id));
    }

    /**
     * 新增产品
     */
    @PreAuthorize("@ss.hasPermi('system:product:add')")
    @Log(title = "产品", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AddProductionReq AddProductionReq)  {
        Long tenantId = getTenantId();
        return toAjax(productService.insertProduct(AddProductionReq,tenantId));
    }

    /**
     * 修改产品
     */
    @PreAuthorize("@ss.hasPermi('system:product:edit')")
    @Log(title = "产品", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UpdateProductionReq updateProductionReq) {
        Long tenantId  = getTenantId();
        return toAjax(productService.updateProduct(updateProductionReq,tenantId));
    }

    /**
     * 删除产品
     */
    @PreAuthorize("@ss.hasPermi('system:product:remove')")
    @Log(title = "产品", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(productService.deleteProductByIds(ids));
    }

    /**
     * 同步产品信息
     */
    @PreAuthorize("@ss.hasPermi('system:product:sync')")
    @Log(title = "产品", businessType = BusinessType.OTHER)
    @PostMapping("/sync")
    public AjaxResult sync() {
        Long tenantId = getTenantId();
        return toAjax(productService.syncProduct(tenantId));
    }

    /**
     * 获取产品详细信息
     */
    @GetMapping("/getUnitList")
    public AjaxResult getUnitList() {
        return AjaxResult.success(productService.getUnitList());
    }
}
