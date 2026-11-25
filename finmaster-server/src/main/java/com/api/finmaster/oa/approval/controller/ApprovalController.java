package com.qykj.finmaster.oa.approval.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.approval.domain.OaApprovalReminder;
import com.qykj.finmaster.oa.approval.domain.req.ApproveReq;
import com.qykj.finmaster.oa.approval.domain.req.SaveDefinitionReq;
import com.qykj.finmaster.oa.approval.domain.req.SubmitApprovalReq;
import com.qykj.finmaster.oa.approval.domain.resp.ApprovalInstanceResp;
import com.qykj.finmaster.oa.approval.domain.ApprovalDefinition;
import com.qykj.finmaster.oa.approval.service.ApprovalServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批管理Controller
 */
@RestController
@RequestMapping("/oa/approval")
public class ApprovalController {

    @Autowired
    private ApprovalServiceImpl approvalService;

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
     * 查询审批定义列表
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:list')")
    @GetMapping("/definition/list")
    public AjaxResult definitionList() {
        Long tenantId = getTenantId();
        List<ApprovalDefinition> list = approvalService.getDefinitionList(tenantId);
        return AjaxResult.success(list);
    }

    /**
     * 新增审批定义
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:add')")
    @PostMapping("/definition")
    public AjaxResult addDefinition(@RequestBody SaveDefinitionReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = approvalService.saveDefinition(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 更新审批定义
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:edit')")
    @PutMapping("/definition")
    public AjaxResult updateDefinition(@RequestBody SaveDefinitionReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = approvalService.saveDefinition(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 删除审批定义
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:remove')")
    @DeleteMapping("/definition/{id}")
    public AjaxResult deleteDefinition(@PathVariable Long id) {
        boolean result = approvalService.deleteDefinition(id);
        return result ? AjaxResult.success() : AjaxResult.error("删除失败");
    }

    /**
     * 提交审批
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:submit')")
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody SubmitApprovalReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long instanceId = approvalService.submit(req.getBusinessType(), req.getBusinessId(), req.getTitle(), tenantId, loginUser);
        return AjaxResult.success(instanceId);
    }

    /**
     * 审批/拒绝
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:approve')")
    @PostMapping("/approve")
    public AjaxResult approve(@RequestBody ApproveReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        String status = approvalService.approve(req, tenantId, loginUser);
        return AjaxResult.success(status);
    }

    /**
     * 我的待审批
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:list')")
    @GetMapping("/my-pending")
    public AjaxResult myPending() {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        List<ApprovalInstanceResp> list = approvalService.getMyPending(tenantId, loginUser);
        return AjaxResult.success(list);
    }

    /**
     * 我提交的审批
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:list')")
    @GetMapping("/my-submitted")
    public AjaxResult mySubmitted() {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        List<ApprovalInstanceResp> list = approvalService.getMySubmitted(tenantId, loginUser);
        return AjaxResult.success(list);
    }

    /**
     * 审批实例详情
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:query')")
    @GetMapping("/instance/{id}")
    public AjaxResult instanceDetail(@PathVariable Long id) {
        ApprovalInstanceResp resp = approvalService.getInstanceDetail(id);
        return AjaxResult.success(resp);
    }

    /**
     * 撤回审批（仅申请人可操作，PENDING状态）
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:submit')")
    @PostMapping("/recall/{instanceId}")
    public AjaxResult recall(@PathVariable Long instanceId) {
        LoginUser loginUser = getLoginUser();
        approvalService.recall(instanceId, loginUser);
        return AjaxResult.success();
    }

    /**
     * 催办（仅申请人可操作，PENDING状态）
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:submit')")
    @PostMapping("/remind/{instanceId}")
    public AjaxResult remind(@PathVariable Long instanceId) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        approvalService.sendReminder(instanceId, tenantId, loginUser);
        return AjaxResult.success("催办成功");
    }

    /**
     * 催办历史
     */
    @PreAuthorize("@ss.hasPermi('oa:approval:query')")
    @GetMapping("/remind-history/{instanceId}")
    public AjaxResult reminderHistory(@PathVariable Long instanceId) {
        List<OaApprovalReminder> list = approvalService.getReminderHistory(instanceId);
        return AjaxResult.success(list);
    }
}
