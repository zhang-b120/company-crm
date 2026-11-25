package com.qykj.finmaster.customer.customerInfoLog.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.customer.customerInfoLog.domain.req.AddCustomerLogReq;
import com.qykj.finmaster.customer.customerInfoLog.domain.req.GetCustomerLogPageReq;
import com.qykj.finmaster.customer.customerInfoLog.domain.req.UpdateCustomerLogReq;
import com.qykj.finmaster.customer.customerInfoLog.service.CustomerLogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 客户操作日志Controller
 */
@RestController
@RequestMapping("/customer/log")
public class CustomerLogController extends BaseController {
    @Autowired
    private CustomerLogServiceImpl customerLogService;

    /**
     * 查询客户操作日志列表
     */
    @PreAuthorize("@ss.hasPermi('customer:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetCustomerLogPageReq req) {
        return getDataTable(customerLogService.selectCustomerLogList(req));
    }

    /**
     * 获取客户操作日志详细信息
     */
    @PreAuthorize("@ss.hasPermi('customer:log:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(customerLogService.selectCustomerLogById(id));
    }

    /**
     * 新增客户操作日志
     */
    @PreAuthorize("@ss.hasPermi('customer:log:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddCustomerLogReq req) {
        return toAjax(customerLogService.insertCustomerLog(req, getTenantId(), getLoginUser()));
    }

    /**
     * 修改客户操作日志
     */
    @PreAuthorize("@ss.hasPermi('customer:log:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody UpdateCustomerLogReq req) {
        return toAjax(customerLogService.updateCustomerLog(req, getTenantId(), getLoginUser()));
    }

    /**
     * 删除客户操作日志
     */
    @PreAuthorize("@ss.hasPermi('customer:log:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String ids) {
        return toAjax(customerLogService.deleteCustomerLogByIds(ids));
    }
}
