package com.qykj.finmaster.oa.expense.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.expense.domain.OaExpense;
import com.qykj.finmaster.oa.expense.domain.req.AddExpenseReq;
import com.qykj.finmaster.oa.expense.domain.req.GetExpenseListReq;
import com.qykj.finmaster.oa.expense.service.ExpenseServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 报销管理Controller
 */
@RestController
@RequestMapping("/oa/expense")
public class ExpenseController {

    @Autowired
    private ExpenseServiceImpl expenseService;

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

    /**
     * 查询报销列表
     */
    @PreAuthorize("@ss.hasPermi('oa:expense:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetExpenseListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaExpense> list = expenseService.getList(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 新增报销申请
     */
    @PreAuthorize("@ss.hasPermi('oa:expense:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddExpenseReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = expenseService.addExpense(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 提交报销申请（发起审批）
     */
    @PreAuthorize("@ss.hasPermi('oa:expense:submit')")
    @PostMapping("/submit/{id}")
    public AjaxResult submit(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long instanceId = expenseService.submitExpense(id, tenantId, loginUser);
        return AjaxResult.success(instanceId);
    }

    /**
     * 获取报销详情（含明细）
     */
    @PreAuthorize("@ss.hasPermi('oa:expense:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        Map<String, Object> detail = expenseService.getDetail(id);
        return AjaxResult.success(detail);
    }

    /**
     * 确认打款
     */
    @PreAuthorize("@ss.hasPermi('oa:expense:confirmPaid')")
    @PostMapping("/confirm-paid/{id}")
    public AjaxResult confirmPaid(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        expenseService.confirmPaid(id, loginUser);
        return AjaxResult.success();
    }

    /**
     * 批量确认打款
     */
    @PreAuthorize("@ss.hasPermi('oa:expense:confirmPaid')")
    @PostMapping("/batch-confirm-paid")
    public AjaxResult batchConfirmPaid(@RequestBody Map<String, List<Long>> body) {
        LoginUser loginUser = getLoginUser();
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return AjaxResult.error("请选择要打款的记录");
        }
        Map<String, Object> result = expenseService.batchConfirmPaid(ids, loginUser);
        return AjaxResult.success(result);
    }
}
