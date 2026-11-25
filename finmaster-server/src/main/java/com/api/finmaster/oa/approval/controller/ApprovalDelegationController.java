package com.qykj.finmaster.oa.approval.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.approval.domain.OaApprovalDelegation;
import com.qykj.finmaster.oa.approval.domain.req.AddDelegationReq;
import com.qykj.finmaster.oa.approval.domain.req.GetDelegationListReq;
import com.qykj.finmaster.oa.approval.service.ApprovalDelegationServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批委托Controller
 */
@RestController
@RequestMapping("/oa/delegation")
public class ApprovalDelegationController {

    @Autowired
    private ApprovalDelegationServiceImpl delegationService;

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
     * 查询我的委托列表
     */
    @PreAuthorize("@ss.hasPermi('oa:delegation:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetDelegationListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaApprovalDelegation> list = delegationService.getMyDelegations(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 新增委托
     */
    @PreAuthorize("@ss.hasPermi('oa:delegation:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddDelegationReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = delegationService.addDelegation(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 更新委托
     */
    @PreAuthorize("@ss.hasPermi('oa:delegation:edit')")
    @PutMapping
    public AjaxResult update(@RequestBody AddDelegationReq req) {
        LoginUser loginUser = getLoginUser();
        delegationService.updateDelegation(req, loginUser);
        return AjaxResult.success();
    }

    /**
     * 删除委托
     */
    @PreAuthorize("@ss.hasPermi('oa:delegation:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        delegationService.deleteDelegation(id);
        return AjaxResult.success();
    }
}
