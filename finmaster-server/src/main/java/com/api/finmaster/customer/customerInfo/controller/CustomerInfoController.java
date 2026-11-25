package com.qykj.finmaster.customer.customerInfo.controller;

import com.qykj.finmaster.common.annotation.Log;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.enums.BusinessType;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import com.qykj.finmaster.customer.customerInfo.domain.req.AddCustomerInfoReq;
import com.qykj.finmaster.customer.customerInfo.domain.req.AssignCustomerInfoReq;
import com.qykj.finmaster.customer.customerInfo.domain.req.GetCustomerInfoReq;
import com.qykj.finmaster.customer.customerInfo.domain.req.UpdateCustomerInfoReq;
import com.qykj.finmaster.customer.customerInfo.domain.resp.GetCustomerInfoResp;
import com.qykj.finmaster.customer.customerInfo.domain.resp.GetCustomerInfoPageListResp;
import com.qykj.finmaster.customer.customerInfo.service.CustomerInfoServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Date;

/**
 * 客户信息Controller
 */
@RestController
@RequestMapping("/customer")
public class CustomerInfoController extends BaseController {

    @Autowired
    private CustomerInfoServiceImpl customerInfoService;

    /**
     * 查询客户列表
     */
    @PreAuthorize("@ss.hasPermi('customer:info:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetCustomerInfoReq getCustomerInfoReq) {
        Long tenantId = getTenantId();
        getCustomerInfoReq.startPage();
        List<GetCustomerInfoPageListResp> list = customerInfoService.selectCustomerInfoList(getCustomerInfoReq, tenantId);
        return getDataTable(list);
    }

    /**
     * 导出客户列表
     */
    @PreAuthorize("@ss.hasPermi('customer:info:export')")
    @Log(title = "客户信息", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(HttpServletResponse response, GetCustomerInfoReq getCustomerInfoReq) {
        Long tenantId = getTenantId();
        List<GetCustomerInfoPageListResp> list = customerInfoService.selectCustomerInfoList(getCustomerInfoReq, tenantId);
        ExcelUtil<GetCustomerInfoPageListResp> util = new ExcelUtil<>(GetCustomerInfoPageListResp.class);
        util.exportExcel(response, list, "客户数据");
    }

    /**
     * 获取客户详细信息
     */
    @PreAuthorize("@ss.hasPermi('customer:info:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        GetCustomerInfoResp resp = customerInfoService.selectCustomerInfoById(id);
        return AjaxResult.success(resp);
    }

    /**
     * 客户全生命周期时间轴：合并"线索阶段"+"客户阶段"的所有跟进/操作日志
     * 按时间倒序返回
     */
    @PreAuthorize("@ss.hasPermi('customer:info:query')")
    @GetMapping(value = "/{id}/timeline")
    public AjaxResult getTimeline(@PathVariable("id") Long id) {
        return AjaxResult.success(customerInfoService.selectCustomerTimeline(id));
    }

    /**
     * 新增客户
     */
    @PreAuthorize("@ss.hasPermi('customer:info:add')")
    @Log(title = "客户信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AddCustomerInfoReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(customerInfoService.insertCustomerInfo(req, tenantId, loginUser));
    }

    /**
     * 修改客户
     */
    @PreAuthorize("@ss.hasPermi('customer:info:edit')")
    @Log(title = "客户信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UpdateCustomerInfoReq updateCustomerInfoReq) {
        Long tenantId = getTenantId();
        return toAjax(customerInfoService.updateCustomerInfo(updateCustomerInfoReq, tenantId, getLoginUser()));
    }

    /**
     * 删除客户
     */
    @PreAuthorize("@ss.hasPermi('customer:info:remove')")
    @Log(title = "客户信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        return toAjax(customerInfoService.deleteCustomerInfoById(id));
    }

    /**
     * 批量删除客户
     */
    @PreAuthorize("@ss.hasPermi('customer:info:remove')")
    @Log(title = "客户信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/batch/{ids}")
    public AjaxResult batchRemove(@PathVariable String ids) {
        return toAjax(customerInfoService.deleteCustomerInfoByIds(ids));
    }

    /**
     * 转入公海
     */
    @PreAuthorize("@ss.hasPermi('customer:info:transfer')")
    @Log(title = "客户信息", businessType = BusinessType.UPDATE)
    @PutMapping("/transfer/{id}")
    public AjaxResult transfer(@PathVariable("id") Long id) {
        Long tenantId = getTenantId();
        return toAjax(customerInfoService.transferToPublicSea(id, tenantId, getLoginUser()));
    }

    /**
     * 公海批量废弃（逻辑删除）
     */
    @PreAuthorize("@ss.hasPermi('customer:info:discard')")
    @Log(title = "客户信息", businessType = BusinessType.DELETE)
    @PostMapping("/discard")
    public AjaxResult discard(@RequestBody List<Long> ids) {
        Long tenantId = getTenantId();
        return toAjax(customerInfoService.discardPublicSeaCustomers(ids, tenantId, getLoginUser()));
    }

    /**
     * 更新最近跟进信息
     */
    @PreAuthorize("@ss.hasPermi('customer:info:edit')")
    @Log(title = "客户信息", businessType = BusinessType.UPDATE)
    @PutMapping("/follow/{id}")
    public AjaxResult updateFollowInfo(@PathVariable Long id, @RequestParam String content, @RequestParam Date nextFollowTime, @RequestParam String status) {
        Long tenantId = getTenantId();
        int result = customerInfoService.updateLastFollowInfo(id, content, nextFollowTime, status, tenantId);
        return toAjax(result);
    }

    /**
     * 认领客户
     */
    @PreAuthorize("@ss.hasPermi('customer:info:claim')")
    @Log(title = "客户信息", businessType = BusinessType.UPDATE)
    @PostMapping("/claim/{id}")
    public AjaxResult claimCustomer(@PathVariable("id") Long id) {
        Long tenantId = getTenantId();
        return toAjax(customerInfoService.claimCustomer(id, tenantId));
    }

    /**
     * 批量分配客户
     */
    @PreAuthorize("@ss.hasPermi('customer:info:assign')")
    @PostMapping("/assign")
    public AjaxResult assign(@RequestBody AssignCustomerInfoReq req) {
        return toAjax(customerInfoService.assignCustomerInfo(req, getLoginUser(), getTenantId()));
    }
}
