package com.qykj.finmaster.oa.makeup.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.makeup.domain.OaMakeupClockin;
import com.qykj.finmaster.oa.makeup.domain.req.AddMakeupClockinReq;
import com.qykj.finmaster.oa.makeup.domain.req.GetMakeupClockinListReq;
import com.qykj.finmaster.oa.makeup.service.MakeupClockinServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 补卡管理Controller
 */
@RestController
@RequestMapping("/oa/makeup-clockin")
public class MakeupClockinController {

    @Autowired
    private MakeupClockinServiceImpl makeupClockinService;

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
     * 查询补卡列表
     */
    @PreAuthorize("@ss.hasPermi('oa:makeup:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetMakeupClockinListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaMakeupClockin> list = makeupClockinService.getList(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 获取补卡详情
     */
    @PreAuthorize("@ss.hasPermi('oa:makeup:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        OaMakeupClockin makeup = makeupClockinService.getDetail(id);
        return AjaxResult.success(makeup);
    }

    /**
     * 新增补卡申请
     */
    @PreAuthorize("@ss.hasPermi('oa:makeup:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddMakeupClockinReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = makeupClockinService.addMakeup(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 提交补卡申请（发起审批）
     */
    @PreAuthorize("@ss.hasPermi('oa:makeup:submit')")
    @PostMapping("/submit/{id}")
    public AjaxResult submit(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long instanceId = makeupClockinService.submitMakeup(id, tenantId, loginUser);
        return AjaxResult.success(instanceId);
    }
}
