package com.qykj.finmaster.customer.company.UaController;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.customer.company.domain.req.ChangeCustomerCompanyReq;
import com.qykj.finmaster.customer.company.domain.req.GetCustomerCompanyListReq;
import com.qykj.finmaster.customer.company.domain.req.UaAddCustomerCompanyReq;
import com.qykj.finmaster.customer.company.domain.req.UaUpdateCustomerCompanyByTaxNumberReq;
import jakarta.validation.Valid;
import com.qykj.finmaster.customer.company.service.CustomerCompanyServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@RestController
@RequestMapping("/ua/customer/company")
public class UaCustomerCompanyController {

    @Autowired
    private CustomerCompanyServiceImpl customerCompanyService;

    /**
     * 小程序新增公司
     * @param req
     * @return
     */
    @PermitAll
    @PostMapping("/uaAddCustomerCompany")
    public AjaxResult uaAddCustomerCompany(@RequestBody UaAddCustomerCompanyReq req){
        return AjaxResult.success(customerCompanyService.uaAddCustomerCompany(req));
    }

    /**
     * 获取当前用户所有公司
     * @param req
     * @return
     */
    @PermitAll
    @PostMapping("/getCustomerCompanyList")
    public AjaxResult getCustomerCompanyList(@RequestBody GetCustomerCompanyListReq req){
        return AjaxResult.success(customerCompanyService.getCustomerCompanyList(req));
    }

    /**
     * 切换公司
     * @param req
     * @return
     */
    @PermitAll
    @PostMapping("/changeCustomerCompany")
    public AjaxResult changeCustomerCompany(@RequestBody ChangeCustomerCompanyReq req){
        return AjaxResult.success(customerCompanyService.changeCustomerCompany(req.getCompanyId(), req.getOpenId()));
    }

    @PermitAll
    @PostMapping("/createExperienceAccountSet")
    public AjaxResult createExperienceAccountSet(@RequestParam Long companyId){
        return AjaxResult.success(customerCompanyService.createExperienceAccountSet(companyId));
    }

    /**
     * 小程序按税号更新公司基本信息
     * 校验：customer_company.openid = openId 且 tax_number = req.taxNumber 才允许
     */
    @PermitAll
    @PostMapping("/updateByTaxNumber")
    public AjaxResult updateByTaxNumber(@RequestBody @Valid UaUpdateCustomerCompanyByTaxNumberReq req){
        return customerCompanyService.uaUpdateByTaxNumber(req)
                ? AjaxResult.success()
                : AjaxResult.error("更新失败");
    }

    /**
     * 按税号查询公司信息（小程序新增公司预填用）
     * 优先级：本地档案 LOCAL（含法人证件号/手机号）→ 天眼查 TYC（仅工商公开信息）→ NONE
     * 小程序未登录场景按租户为空查询本地，等价于不限租户取最新一条；命中 TYC 时只返回公开字段
     */
    @PermitAll
    @GetMapping("/lookupByTaxNumber")
    public AjaxResult lookupByTaxNumber(@RequestParam("taxNumber") String taxNumber){
        return AjaxResult.success(customerCompanyService.lookupByTaxNumber(taxNumber, null));
    }
}