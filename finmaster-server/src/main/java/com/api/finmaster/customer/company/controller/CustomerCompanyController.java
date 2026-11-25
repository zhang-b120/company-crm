package com.qykj.finmaster.customer.company.controller;

import com.qykj.finmaster.common.annotation.Log;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.enums.BusinessType;
import com.qykj.finmaster.customer.company.domain.req.*;
import com.qykj.finmaster.customer.company.service.CustomerCompanyServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.qykj.finmaster.customer.company.domain.resp.GetCustomerCompanyPageResp;

/**
 * 客户公司信息Controller
 */
@RestController
@RequestMapping("/customer/company")
public class CustomerCompanyController extends BaseController {


    @Autowired
    private CustomerCompanyServiceImpl customerCompanyService;

    /**
     * 查询客户公司信息列表
     */
    @PreAuthorize("@ss.hasPermi('customer:company:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetCustomerCompanyPageReq req) {
        Long tenantId = getTenantId();
        return getDataTable(customerCompanyService.selectCustomerCompanyList(req, tenantId));
    }

    /**
     * 获取客户公司信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('customer:company:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        Long tenantId = getTenantId();
        return AjaxResult.success(customerCompanyService.selectCustomerCompanyById(id, tenantId));

    }

    /**
     * 新增客户公司信息
     */
    @PreAuthorize("@ss.hasPermi('customer:company:add')")
    @Log(title = "客户公司信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AddCustomerCompanyReq req) {
        Long tenantId = getTenantId();
        return AjaxResult.success(customerCompanyService.addCustomerCompany(req, tenantId, getLoginUser()));
    }

    /**
     * 修改客户公司信息
     */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "客户公司信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody UpdateCustomerCompanyReq req) {
        LoginUser loginUser = super.getLoginUser();
        return toAjax(customerCompanyService.updateCustomerCompany(req, getTenantId(), loginUser));
    }

    /**
     * 修改客户公司信息
     */
    @PreAuthorize("@ss.hasPermi('customer-service:company:edit')")
    @Log(title = "修改客户公司信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updateCompanyInfo")
    public AjaxResult updateCompanyInfo(@RequestBody UpdateCustomerCompanyReq req) {
        Boolean result = customerCompanyService.updateCompanyInfo(req, getTenantId(), this.getLoginUser());
        return result ? AjaxResult.success() : AjaxResult.error("修改客户公司信息失败");
    }

    /**
     * 按税号更新公司基本信息（租户内唯一定位）
     */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "按税号更新公司", businessType = BusinessType.UPDATE)
    @PutMapping("/by-tax-number")
    public AjaxResult updateByTaxNumber(@RequestBody @jakarta.validation.Valid UpdateCustomerCompanyByTaxNumberReq req) {
        return customerCompanyService.updateByTaxNumber(req, getTenantId(), getLoginUser())
                ? AjaxResult.success()
                : AjaxResult.error("更新失败");
    }

    /**
     * 从企查查刷新公司的工商信息（注册地址 / 成立日期 / 工商状态 / 法人）
     */
    @PreAuthorize("@ss.hasPermi('customer:company:edit')")
    @Log(title = "刷新工商信息", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/refresh-qcc")
    public AjaxResult refreshQccInfo(@PathVariable("id") Long id) {
        return AjaxResult.success(customerCompanyService.refreshQccInfo(id, getTenantId(), getLoginUser()));
    }

    /**
     * 验证税密
     */
    @PreAuthorize("@ss.hasPermi('customer-service:company:verifyTax')")
    @Log(title = "验证税密", businessType = BusinessType.UPDATE)
    @PostMapping("/verifyTax")
    public AjaxResult verifyTax(@RequestBody List<Long> ids) {
        Boolean result = customerCompanyService.verifyTax(ids);
        return result ? AjaxResult.success() : AjaxResult.error("验证税密失败");
    }

    /**
     * 取消验证税密
     */
    @PreAuthorize("@ss.hasPermi('customer-service:company:cancelVerifyTax')")
    @Log(title = "取消验证税密", businessType = BusinessType.UPDATE)
    @PostMapping("/cancelVerifyTax")
    public AjaxResult cancelVerifyTax(@RequestBody List<Long> ids) {
        Long tenantId = getTenantId();
        Boolean result = customerCompanyService.cancelVerifyTax(ids);
        return result ? AjaxResult.success() : AjaxResult.error("取消验证税密失败");
    }

    /**
     * 修改公司状态
     */
    @PreAuthorize("@ss.hasPermi('customer-service:company:edit')")
    @Log(title = "修改公司状态", businessType = BusinessType.UPDATE)
    @PutMapping("/updateCompanyStatus")
    public AjaxResult updateCompanyStatus(@RequestParam Long id, @RequestParam String companyStatus,
                                           @RequestParam(required = false) String statusRemark) {
        Boolean result = customerCompanyService.updateCompanyStatus(id, companyStatus, statusRemark);
        return result ? AjaxResult.success() : AjaxResult.error("修改公司状态失败");
    }

    /**
     * 根据客户ID查询公司信息列表
     */
    @PreAuthorize("@ss.hasPermi('customer:company:list')")
    @GetMapping("/listByCustomerId/{customerId}")
    public AjaxResult listByCustomerId(@PathVariable("customerId") Long customerId) {
        Long tenantId = getTenantId();
        return AjaxResult.success(customerCompanyService.selectCustomerCompanyListByCustomerId(customerId, tenantId));
    }

    /**
     * 查询客户公司信息列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('customer-service:company:list')")
    @PostMapping("/getCustomerCompanyList")
    public TableDataInfo getCustomerCompanyList(@RequestBody GetCustomerCompanyPageReq req) {
        List<GetCustomerCompanyPageResp> list = customerCompanyService.getCustomerCompanyList(req);
        return getDataTable(list);
    }

    /**
     * 查询客户公司操作日志列表
     */
    @PreAuthorize("@ss.hasPermi('customer:company:log:list')")
    @GetMapping("/log/list")
    public TableDataInfo listCompanyLog(GetCustomerCompanyLogReq req) {
        Long tenantId = getTenantId();
        return getDataTable(customerCompanyService.selectCustomerCompanyLogList(req,tenantId));
    }

    /**
     * 新增客户公司操作日志
     */
    @PreAuthorize("@ss.hasPermi('customer:company:log:add')")
    @PostMapping("/log")
    public AjaxResult addCompanyLog(@RequestBody AddCustomerCompanyLogReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(customerCompanyService.insertCustomerCompanyLog(req, tenantId, loginUser));
    }

    /**
     * 开通开票服务
     */
    @PreAuthorize("@ss.hasPermi('customer-service:company:invoicing')")
    @Log(title = "开通开票服务", businessType = BusinessType.UPDATE)
    @PostMapping("/invoicing")
    public AjaxResult invoicing(@RequestBody List<Long> ids) {
        return toAjax(customerCompanyService.invoicing(ids));
    }

    @PreAuthorize("@ss.hasPermi('customer-service:company:cancelInvoicing')")
    @Log(title = "取消开票服务", businessType = BusinessType.UPDATE)
    @PostMapping("/cancelInvoicing")
    public AjaxResult cancelInvoicing(@RequestBody List<Long> ids) {
        return toAjax(customerCompanyService.cancelInvoicing(ids));
    }

    /**
     * 修改客户公司信息
     */
    @PreAuthorize("@ss.hasPermi('customer:company:remove')")
    @Log(title = "客户公司信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable("id") Long id) {
        return toAjax(customerCompanyService.deleteCustomerCompanyById(id));
    }

}
