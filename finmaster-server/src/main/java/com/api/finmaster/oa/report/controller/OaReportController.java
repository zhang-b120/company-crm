package com.qykj.finmaster.oa.report.controller;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.report.domain.resp.ExpenseReportResp;
import com.qykj.finmaster.oa.report.domain.resp.PayrollReportResp;
import com.qykj.finmaster.oa.report.service.OaReportServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OA报表Controller
 */
@RestController
@RequestMapping("/oa/report")
public class OaReportController {

    @Autowired
    private OaReportServiceImpl reportService;

    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (LoginUser) auth.getPrincipal();
    }

    private Long getTenantId() {
        return getLoginUser().getTenantId();
    }

    /**
     * 费用分析报表
     */
    @PreAuthorize("@ss.hasPermi('oa:report:expense')")
    @GetMapping("/expense")
    public AjaxResult expenseReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long tenantId = getTenantId();
        ExpenseReportResp resp = reportService.getExpenseReport(startDate, endDate, tenantId);
        return AjaxResult.success(resp);
    }

    /**
     * 人力成本报表
     */
    @PreAuthorize("@ss.hasPermi('oa:report:payroll')")
    @GetMapping("/payroll")
    public AjaxResult payrollReport(
            @RequestParam(required = false) String startMonth,
            @RequestParam(required = false) String endMonth) {
        Long tenantId = getTenantId();
        PayrollReportResp resp = reportService.getPayrollReport(startMonth, endMonth, tenantId);
        return AjaxResult.success(resp);
    }
}
