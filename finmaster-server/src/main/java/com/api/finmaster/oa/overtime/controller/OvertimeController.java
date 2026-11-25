package com.qykj.finmaster.oa.overtime.controller;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.overtime.domain.OaOvertimeRecord;
import com.qykj.finmaster.oa.overtime.service.OvertimeServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 加班记录Controller
 */
@RestController
@RequestMapping("/oa/overtime")
public class OvertimeController {

    @Autowired
    private OvertimeServiceImpl overtimeService;

    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (LoginUser) auth.getPrincipal();
    }

    /**
     * 查询加班记录列表
     */
    @PreAuthorize("@ss.hasPermi('oa:overtime:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        LoginUser loginUser = getLoginUser();
        List<OaOvertimeRecord> list = overtimeService.getMyList(loginUser.getTenantId(), loginUser);
        return AjaxResult.success(list);
    }

    /**
     * 查询可用（未使用）加班记录（用于申请调休时选择）
     */
    @PreAuthorize("@ss.hasPermi('oa:overtime:list')")
    @GetMapping("/available")
    public AjaxResult available() {
        LoginUser loginUser = getLoginUser();
        List<OaOvertimeRecord> list = overtimeService.getAvailableList(loginUser.getTenantId(), loginUser);
        return AjaxResult.success(list);
    }

    /**
     * 新增加班记录
     */
    @PreAuthorize("@ss.hasPermi('oa:overtime:add')")
    @PostMapping
    public AjaxResult add(@RequestBody OaOvertimeRecord req) {
        LoginUser loginUser = getLoginUser();
        Long id = overtimeService.addOvertime(req, loginUser.getTenantId(), loginUser);
        return AjaxResult.success(id);
    }
}
