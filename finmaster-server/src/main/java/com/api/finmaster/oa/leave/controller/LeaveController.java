package com.qykj.finmaster.oa.leave.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.leave.domain.OaLeave;
import com.qykj.finmaster.oa.leave.domain.OaLeaveQuota;
import com.qykj.finmaster.oa.leave.domain.req.AddLeaveReq;
import com.qykj.finmaster.oa.leave.domain.req.GetLeaveListReq;
import com.qykj.finmaster.oa.leave.service.LeaveServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 请假管理Controller
 */
@RestController
@RequestMapping("/oa/leave")
public class LeaveController {

    @Autowired
    private LeaveServiceImpl leaveService;

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
     * 查询请假列表
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetLeaveListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaLeave> list = leaveService.getList(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 新增请假申请
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddLeaveReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = leaveService.addLeave(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 提交请假申请（发起审批）
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:submit')")
    @PostMapping("/submit/{id}")
    public AjaxResult submit(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long instanceId = leaveService.submitLeave(id, tenantId, loginUser);
        return AjaxResult.success(instanceId);
    }

    /**
     * 获取请假详情
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        OaLeave leave = leaveService.getDetail(id);
        return AjaxResult.success(leave);
    }

    /**
     * 查询我的当年假期余额
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/quota/my")
    public AjaxResult myQuota() {
        LoginUser loginUser = getLoginUser();
        List<OaLeaveQuota> quotas = leaveService.getMyQuota(getTenantId(), loginUser.getUserId());
        return AjaxResult.success(quotas);
    }

    /**
     * 查询所有假期配额（管理员）
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:list')")
    @GetMapping("/quota/list")
    public AjaxResult quotaList(@RequestParam(required = false) Integer year) {
        List<OaLeaveQuota> quotas = leaveService.getAllQuota(getTenantId(), year);
        return AjaxResult.success(quotas);
    }

    /**
     * 设置假期配额
     */
    @PreAuthorize("@ss.hasPermi('oa:leave:edit')")
    @PostMapping("/quota")
    public AjaxResult saveQuota(@RequestBody OaLeaveQuota quota) {
        quota.setTenantId(getTenantId());
        leaveService.saveQuota(quota);
        return AjaxResult.success();
    }
}
