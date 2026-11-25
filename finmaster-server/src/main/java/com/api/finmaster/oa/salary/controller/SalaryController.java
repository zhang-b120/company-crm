package com.qykj.finmaster.oa.salary.controller;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.salary.domain.OaDeductionRule;
import com.qykj.finmaster.oa.salary.domain.OaSocialInsuranceConfig;
import com.qykj.finmaster.oa.salary.domain.SalaryConfig;
import com.qykj.finmaster.oa.salary.domain.SalaryRecord;
import com.qykj.finmaster.oa.salary.domain.req.CalculateSalaryReq;
import com.qykj.finmaster.oa.salary.domain.req.SaveSalaryConfigReq;
import com.qykj.finmaster.oa.salary.service.SalaryServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 薪资管理Controller
 */
@RestController
@RequestMapping("/oa/salary")
public class SalaryController {

    @Autowired
    private SalaryServiceImpl salaryService;

    /**
     * 获取当前登录用户
     */
    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (LoginUser) auth.getPrincipal();
    }

    /**
     * 获取租户ID
     */
    private Long getTenantId() {
        LoginUser loginUser = getLoginUser();
        return loginUser.getTenantId();
    }

    // ==================== 薪资配置相关 ====================

    /**
     * 查询薪资配置列表
     */
    @PreAuthorize("@ss.hasPermi('oa:salary:list')")
    @GetMapping("/config/list")
    public AjaxResult configList() {
        Long tenantId = getTenantId();
        List<SalaryConfig> list = salaryService.getSalaryConfigList(tenantId);
        return AjaxResult.success(list);
    }

    /**
     * 新增薪资配置
     */
    @PreAuthorize("@ss.hasPermi('oa:salary:add')")
    @PostMapping("/config")
    public AjaxResult addConfig(@RequestBody SaveSalaryConfigReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = salaryService.saveSalaryConfig(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 更新薪资配置
     */
    @PreAuthorize("@ss.hasPermi('oa:salary:edit')")
    @PutMapping("/config")
    public AjaxResult updateConfig(@RequestBody SaveSalaryConfigReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = salaryService.saveSalaryConfig(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 删除薪资配置
     */
    @PreAuthorize("@ss.hasPermi('oa:salary:remove')")
    @DeleteMapping("/config/{id}")
    public AjaxResult deleteConfig(@PathVariable Long id) {
        boolean result = salaryService.deleteSalaryConfig(id);
        return result ? AjaxResult.success() : AjaxResult.error("删除失败");
    }

    // ==================== 薪资核算相关 ====================

    /**
     * 月度薪资核算
     */
    @PreAuthorize("@ss.hasPermi('oa:salary:calculate')")
    @PostMapping("/calculate")
    public AjaxResult calculate(@RequestBody CalculateSalaryReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        List<SalaryRecord> records = salaryService.calculateMonthly(req.getSalaryMonth(), tenantId, loginUser);
        return AjaxResult.success(records);
    }

    /**
     * 查询薪资记录列表
     */
    @PreAuthorize("@ss.hasPermi('oa:salary:list')")
    @GetMapping("/record/list")
    public AjaxResult recordList(@RequestParam(required = false) String salaryMonth) {
        Long tenantId = getTenantId();
        List<SalaryRecord> list = salaryService.getSalaryRecordList(salaryMonth, tenantId);
        return AjaxResult.success(list);
    }

    /**
     * 确认薪资
     */
    @PreAuthorize("@ss.hasPermi('oa:salary:confirm')")
    @PostMapping("/record/confirm/{id}")
    public AjaxResult confirm(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        boolean result = salaryService.confirmSalary(id, loginUser);
        return result ? AjaxResult.success() : AjaxResult.error("确认失败");
    }

    /**
     * 发送工资条
     */
    @PreAuthorize("@ss.hasPermi('oa:salary:confirm')")
    @PostMapping("/record/send-payslip/{id}")
    public AjaxResult sendPayslip(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        salaryService.sendPayslip(id, loginUser);
        return AjaxResult.success();
    }

    // ==================== 社保配置 ====================

    @PreAuthorize("@ss.hasPermi('oa:salary:list')")
    @GetMapping("/social-insurance/list")
    public AjaxResult socialInsuranceList() {
        Long tenantId = getTenantId();
        return AjaxResult.success(salaryService.getSocialInsuranceConfigList(tenantId));
    }

    @PreAuthorize("@ss.hasPermi('oa:salary:edit')")
    @PostMapping("/social-insurance")
    public AjaxResult saveSocialInsurance(@RequestBody OaSocialInsuranceConfig req) {
        Long tenantId = getTenantId();
        Long id = salaryService.saveSocialInsuranceConfig(req, tenantId);
        return AjaxResult.success(id);
    }

    @PreAuthorize("@ss.hasPermi('oa:salary:remove')")
    @DeleteMapping("/social-insurance/{id}")
    public AjaxResult deleteSocialInsurance(@PathVariable Long id) {
        boolean result = salaryService.deleteSocialInsuranceConfig(id);
        return result ? AjaxResult.success() : AjaxResult.error("删除失败");
    }

    // ==================== 扣款规则 ====================

    @PreAuthorize("@ss.hasPermi('oa:salary:list')")
    @GetMapping("/deduction-rule")
    public AjaxResult getDeductionRule() {
        Long tenantId = getTenantId();
        return AjaxResult.success(salaryService.getDeductionRule(tenantId));
    }

    @PreAuthorize("@ss.hasPermi('oa:salary:edit')")
    @PostMapping("/deduction-rule")
    public AjaxResult saveDeductionRule(@RequestBody OaDeductionRule req) {
        Long tenantId = getTenantId();
        OaDeductionRule result = salaryService.saveDeductionRule(req, tenantId);
        return AjaxResult.success(result);
    }
}
