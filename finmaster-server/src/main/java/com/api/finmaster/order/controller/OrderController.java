package com.qykj.finmaster.order.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.order.domain.dto.OrderDetailInfo;
import com.qykj.finmaster.order.domain.req.*;
import com.qykj.finmaster.order.domain.resp.GetOrderListExportResp;
import com.qykj.finmaster.order.domain.resp.GetOrderListResp;
import com.qykj.finmaster.order.service.OrderServiceImpl;
import com.qykj.finmaster.order.service.QuickOrderService;
import com.qykj.finmaster.security.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.qykj.finmaster.common.annotation.Log;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.enums.BusinessType;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 订单Controller
 */
@RestController
@RequestMapping("/order")
public class OrderController extends BaseController {


    @Autowired
    private OrderServiceImpl orderService;

    @Autowired
    private QuickOrderService quickOrderService;

    /**
     * 查询订单列表
     */
    @PreAuthorize("@ss.hasPermi('order:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetOrderListInfoReq getOrderListInfoReq) {
        Long tenantId = getTenantId();
        List<GetOrderListResp> list = orderService.selectOrderList(getOrderListInfoReq, tenantId);
        return getDataTable(list);
    }

    /**
     * 导出订单列表
     */
    @PreAuthorize("@ss.hasPermi('order:order:export')")
    @Log(title = "订单", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(HttpServletResponse response, GetOrderListInfoReq getOrderListInfoReq) {
        Long tenantId = getTenantId();
        List<GetOrderListResp> list = orderService.selectOrderList(getOrderListInfoReq, tenantId);
        //转换格式
        List<GetOrderListExportResp> list2 = orderService.orderAndDetailExport(list);
        ExcelUtil<GetOrderListExportResp> util = new ExcelUtil<>(GetOrderListExportResp.class);
        util.exportExcel(response, list2, "订单数据");
    }

    /**
     * 获取订单详细信息
     */
    @PreAuthorize("@ss.hasPermi('order:order:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        Long tenantId = getTenantId();
        return AjaxResult.success(orderService.selectOrderById(id, tenantId));
    }

    /**
     * 新增订单
     */
    @PreAuthorize("@ss.hasPermi('order:order:add')")
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AddOrderInfoReq addOrderInfoReq) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(orderService.insertOrder(addOrderInfoReq, tenantId, loginUser));
    }

    /**
     * 快速建单（一步创建客户+公司+订单）
     */
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping("/quick")
    public AjaxResult quickCreate(@RequestBody com.qykj.finmaster.order.domain.req.QuickOrderReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return AjaxResult.success(quickOrderService.quickCreate(req, tenantId, loginUser));
    }

    /**
     * 修改订单
     */
    @PreAuthorize("@ss.hasPermi('order:order:edit')")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UpdateOrderInfoReq updateOrderInfoReq) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(orderService.updateOrder(updateOrderInfoReq, loginUser, tenantId));
    }

    /**
     * 删除订单
     */
    @PreAuthorize("@ss.hasPermi('order:order:remove')")
    @Log(title = "订单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        Long tenantId = getTenantId();
        int result = orderService.deleteOrderByIds(ids, tenantId);
        return toAjax(result);
    }

    /**
     * 更新订单状态
     */
    @PreAuthorize("@ss.hasPermi('order:order:edit')")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status/{status}")
    public AjaxResult updateStatus(@PathVariable("id") Long id,
                                   @PathVariable("status") String status,
                                   @RequestBody(required = false) Map<String, BigDecimal> body) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        BigDecimal paidAmount = body != null ? body.get("paidAmount") : null;
        int result = orderService.updateOrderStatus(id, status, paidAmount, tenantId, loginUser);
        return toAjax(result);
    }

    /**
     * 更新订单明细日期
     */
    @PutMapping("/detail")
    public AjaxResult updateOrderDetail(@RequestBody OrderDetailInfo orderDetailInfo) {
        int result = orderService.updateOrderDetail(orderDetailInfo);
        return toAjax(result);
    }

    /**
     * 根据公司获取不可选的业务ID列表（新增订单时，该公司下已存在某业务则其配置的不可新增业务不可选）
     */
    @PreAuthorize("@ss.hasPermi('order:order:list')")
    @GetMapping("/blockedBusinessIds")
    public AjaxResult getBlockedBusinessIds(@RequestParam Long companyId) {
        Long tenantId = getTenantId();
        return AjaxResult.success(orderService.getBlockedBusinessIdsByCompany(companyId, tenantId));
    }

    /**
     * 更新订单明细公司
     */
    @PreAuthorize("@ss.hasPermi('order:order:edit')")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PostMapping("/updateOrderCompany")
    public AjaxResult updateOrderCompany(@RequestParam Long id, @RequestParam Long companyId) {
        LoginUser loginUser = getLoginUser();
        return AjaxResult.success(orderService.updateOrderCompany(id, companyId, loginUser));
    }

    /**
     * 更新订单明细公司
     */
    @PreAuthorize("@ss.hasPermi('order:order:edit')")
    @Log(title = "订单", businessType = BusinessType.UPDATE)
    @PostMapping("/updateOrderLeadMonth")
    public AjaxResult updateOrderLeadMonth(@RequestParam Long id, @RequestParam String leadMonth) {
        LoginUser loginUser = getLoginUser();
        return AjaxResult.success(orderService.updateOrderLeadMonth(id, leadMonth, loginUser));
    }

    /**
     * 新增退款订单
     */
    @PreAuthorize("@ss.hasPermi('order:order:add')")
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping("/addRefundOrder")
    public AjaxResult addRefundOrder(@RequestBody AddRefundOrderReq addRefundOrderReq) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(orderService.addRefundOrder(addRefundOrderReq, tenantId, loginUser));
    }

    /**
     * 新增补款订单（订单类型BALANCE，记录原订单号与明细原明细ID，明细业务强制补款）
     */
    @PreAuthorize("@ss.hasPermi('order:order:add')")
    @Log(title = "订单", businessType = BusinessType.INSERT)
    @PostMapping("/addBalanceOrder")
    public AjaxResult addBalanceOrder(@RequestBody AddBalanceOrderReq addBalanceOrderReq) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(orderService.addBalanceOrder(addBalanceOrderReq, tenantId, loginUser));
    }

    /**
     * 查询退款订单
     */
    @PreAuthorize("@ss.hasPermi('order:order:refundList')")
    @PostMapping("/getRefundOrderPage")
    public TableDataInfo getRefundOrderPage(@RequestBody GetRefundOrderPageReq req) {
        AtomicLong total = new AtomicLong(0L);
        TableDataInfo dataTable = getDataTable(orderService.getRefundOrderPage(req, getTenantId(), total));
        dataTable.setTotal(total.get());
        return dataTable;
    }

    /** 按 refund_order_info.id 查询退款订单流转信息（含银行账户 / 截图 / 状态位） */
    @GetMapping("/refundOrderInfo/{id}")
    public AjaxResult getRefundOrderInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(orderService.getRefundOrderInfo(id));
    }

    /** 实时保存"取消办税"4 个开关到 refund_order_info（工单详情节点 1 用） */
    @PostMapping("/refundOrderInfo/{id}/cancel-flags")
    public AjaxResult saveRefundCancelFlags(@PathVariable("id") Long id,
                                            @RequestBody java.util.Map<String, Object> flags) {
        orderService.saveRefundCancelFlags(id, flags);
        return AjaxResult.success();
    }

    /** 工单审核完成时同步审核退款金额到 Order（refund 类型订单的 totalAmount/finalAmount/paidAmount） */
    @PostMapping("/refundOrder/{orderId}/reviewed-amount")
    public AjaxResult updateReviewedAmount(@PathVariable("orderId") Long orderId,
                                           @RequestParam("amount") java.math.BigDecimal amount) {
        orderService.updateReviewedRefundAmount(orderId, amount);
        return AjaxResult.success();
    }

    /** 退款凭证图片上传：返回 {url} 给 el-upload 自动模式回填 */
    @PostMapping("/refund/upload-image")
    public AjaxResult uploadRefundImage(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) return AjaxResult.error("文件不能为空");
        try {
            String origin = file.getOriginalFilename();
            String suffix = origin != null && origin.contains(".") ? origin.substring(origin.lastIndexOf('.') + 1) : "png";
            String fileName = "REF_" + System.currentTimeMillis() + "_" + new java.util.Random().nextInt(10000) + "." + suffix;
            java.io.File dir = new java.io.File(com.qykj.finmaster.common.core.domain.constansts.FileConstants.FILE_URL_ORDER_REFUND);
            if (!dir.exists()) dir.mkdirs();
            file.transferTo(new java.io.File(dir, fileName));
            // 走后端流式输出，避免依赖外部 Nginx 静态映射或 BASE_URL 域名
            String url = "/finmaster-api/order/refund/image/" + fileName;
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("url", url);
            data.put("name", origin);
            return AjaxResult.success(data);
        } catch (Exception e) {
            return AjaxResult.error("上传失败：" + e.getMessage());
        }
    }

    /** 退款凭证图片流式输出（与上传 URL 配对）放白名单 */
    @GetMapping("/refund/image/{filename:.+}")
    public org.springframework.http.ResponseEntity<org.springframework.core.io.Resource> getRefundImage(@PathVariable String filename) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
        java.io.File f = new java.io.File(com.qykj.finmaster.common.core.domain.constansts.FileConstants.FILE_URL_ORDER_REFUND, filename);
        if (!f.exists() || !f.isFile()) {
            return org.springframework.http.ResponseEntity.notFound().build();
        }
        org.springframework.http.MediaType mediaType = org.springframework.http.MediaType.IMAGE_PNG;
        try {
            String mime = java.nio.file.Files.probeContentType(f.toPath());
            if (mime != null) mediaType = org.springframework.http.MediaType.parseMediaType(mime);
        } catch (java.io.IOException ignored) {}
        return org.springframework.http.ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofDays(7)))
                .body(new org.springframework.core.io.FileSystemResource(f));
    }

    /**
     * 退款订单审核
     */
    @PostMapping("/reviewRefundOrder")
    public AjaxResult reviewRefundOrder(@RequestBody ReviewRefundOrderReq req) {
        return AjaxResult.success(orderService.reviewRefundOrder(req));
    }

    /**
     * 退款转账
     */
    @PostMapping("/refundTransfer")
    public AjaxResult refundTransfer(@RequestPart("req") RefundTransferReq req,
                                     @RequestPart(value = "file", required = false) MultipartFile file) {
        return AjaxResult.success(orderService.refundTransfer(req, file));
    }


    /**
     * 取消办税
     */
    @PostMapping("/refundCancelTax")
    public AjaxResult refundCancelTax(@RequestPart("req") RefundCancelTaxReq req,
                                     @RequestPart(value = "file") MultipartFile file) {
        return AjaxResult.success(orderService.refundCancelTax(req, file));
    }
}
