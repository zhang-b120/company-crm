package com.qykj.finmaster.order.controller;

import java.util.List;

import com.qykj.finmaster.order.domain.req.GetOrderLogInfoReq;
import com.qykj.finmaster.order.domain.resp.OrderLogResp;
import com.qykj.finmaster.order.service.OrderLogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.qykj.finmaster.common.annotation.Log;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.enums.BusinessType;
import com.qykj.finmaster.order.domain.OrderLog;

/**
 * 订单日志Controller
 */
@RestController
@RequestMapping("/system/order/log")
public class OrderLogController extends BaseController {

    @Autowired
    private OrderLogServiceImpl orderLogService;

    /**
     * 查询订单日志列表
     */
    @PreAuthorize("@ss.hasPermi('order:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetOrderLogInfoReq getOrderLogInfoReq) {
        Long tenantId = getTenantId();
        // 设置租户ID
        getOrderLogInfoReq.startPage();
        List<OrderLog> list = orderLogService.selectOrderLogList(getOrderLogInfoReq, tenantId);
        return getDataTable(list);
    }

    /**
     * 获取订单日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('order:log:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(orderLogService.selectOrderLogById(id));
    }

    /**
     * 新增订单日志
     */
    @PreAuthorize("@ss.hasPermi('order:log:add')")
    @Log(title = "订单日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody OrderLogResp orderLogResp) {
        Long tenantId = getTenantId();
        return toAjax(orderLogService.insertOrderLog(orderLogResp, tenantId));
    }

    /**
     * 修改订单日志
     */
    @PreAuthorize("@ss.hasPermi('order:log:edit')")
    @Log(title = "订单日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody OrderLogResp orderLogResp) {
        int result = orderLogService.updateOrderLog(orderLogResp);
        return toAjax(result);
    }

    /**
     * 删除订单日志
     */
    @PreAuthorize("@ss.hasPermi('order:log:remove')")
    @Log(title = "订单日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        Long tenantId = getTenantId();
        int result = orderLogService.deleteOrderLogByIds(ids, tenantId);
        return toAjax(result);
    }
}
