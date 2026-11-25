package com.qykj.finmaster.oa.schedule.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.schedule.domain.OaSchedule;
import com.qykj.finmaster.oa.schedule.domain.req.AddScheduleReq;
import com.qykj.finmaster.oa.schedule.domain.req.GetScheduleListReq;
import com.qykj.finmaster.oa.schedule.service.ScheduleServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 日程管理Controller
 */
@RestController
@RequestMapping("/oa/schedule")
public class ScheduleController {

    @Autowired
    private ScheduleServiceImpl scheduleService;

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
     * 查询日程列表（当前用户）
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetScheduleListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaSchedule> list = scheduleService.getList(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 查询团队日程
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:list')")
    @GetMapping("/team")
    public TableDataInfo teamSchedules(GetScheduleListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaSchedule> list = scheduleService.getTeamSchedules(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 获取日程详情（含参与人）
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        Map<String, Object> detail = scheduleService.getDetail(id);
        return AjaxResult.success(detail);
    }

    /**
     * 新增日程
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddScheduleReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = scheduleService.addSchedule(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 修改日程
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:edit')")
    @PutMapping
    public AjaxResult update(@RequestBody AddScheduleReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        scheduleService.updateSchedule(req, tenantId, loginUser);
        return AjaxResult.success();
    }

    /**
     * 删除日程
     */
    @PreAuthorize("@ss.hasPermi('oa:schedule:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult delete(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return AjaxResult.success();
    }
}
