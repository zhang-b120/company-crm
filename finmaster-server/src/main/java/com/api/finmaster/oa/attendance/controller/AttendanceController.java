package com.qykj.finmaster.oa.attendance.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.attendance.domain.OaAttendance;
import com.qykj.finmaster.oa.attendance.domain.req.AddAttendanceReq;
import com.qykj.finmaster.oa.attendance.domain.req.GetAttendanceListReq;
import com.qykj.finmaster.oa.attendance.service.AttendanceServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 考勤管理Controller
 */
@RestController
@RequestMapping("/oa/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceServiceImpl attendanceService;

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
     * 查询考勤列表
     */
    @PreAuthorize("@ss.hasPermi('oa:attendance:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetAttendanceListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaAttendance> list = attendanceService.getList(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 新增考勤记录
     */
    @PreAuthorize("@ss.hasPermi('oa:attendance:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddAttendanceReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = attendanceService.addAttendance(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 更新考勤记录
     */
    @PreAuthorize("@ss.hasPermi('oa:attendance:edit')")
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @RequestBody AddAttendanceReq req) {
        LoginUser loginUser = getLoginUser();
        boolean result = attendanceService.updateAttendance(id, req, loginUser);
        return result ? AjaxResult.success() : AjaxResult.error("更新失败");
    }

    /**
     * 月度考勤汇总
     */
    @PreAuthorize("@ss.hasPermi('oa:attendance:list')")
    @GetMapping("/monthly-summary")
    public AjaxResult monthlySummary(@RequestParam int year, @RequestParam int month) {
        Long tenantId = getTenantId();
        List<Map<String, Object>> summary = attendanceService.getMonthlySummary(year, month, tenantId);
        return AjaxResult.success(summary);
    }
}
