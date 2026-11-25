package com.qykj.finmaster.customer.company.UaController;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.customer.company.domain.req.UaAddCompanyReq;
import com.qykj.finmaster.customer.company.domain.req.UaBatchImportStockholderReq;
import com.qykj.finmaster.customer.company.service.CompanyLegalPersonService;
import com.qykj.finmaster.customer.company.service.CustomerCompanyServiceImpl;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序端：公司相关操作（新增公司、批量导入股东）
 * 路径结构与 PC 端对齐（PC 端无 /ua 前缀）：
 *   PC  POST /customer/company                          → UA POST /ua/company/add
 *   PC  POST /company/legal-person/batch-import         → UA POST /ua/company/legal-person/batch-import
 */
@RestController
@RequestMapping("/ua/company")
public class UaCompanyController {

    @Autowired
    private CustomerCompanyServiceImpl customerCompanyService;

    @Autowired
    private CompanyLegalPersonService legalPersonService;

    /**
     * 小程序新增公司（按 openId 关联到客户）
     */
    @PermitAll
    @PostMapping("/add")
    public AjaxResult add(@RequestBody @Valid UaAddCompanyReq req) {
        return AjaxResult.success(customerCompanyService.uaAddCompany(req));
    }

    /**
     * 小程序批量导入股东（天眼查返回的股东数据）
     */
    @PermitAll
    @PostMapping("/legal-person/batch-import")
    public AjaxResult batchImportStockholders(@RequestBody @Valid UaBatchImportStockholderReq req) {
        int count = legalPersonService.uaBatchImportFromTyc(req.getOpenId(), req.getCompanyId(), req.getStockholders());
        return AjaxResult.success("已导入 " + count + " 条股东信息", count);
    }
}
