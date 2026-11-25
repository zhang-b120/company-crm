package com.qykj.finmaster.analytics.oa.controller;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.oa.dto.OaAnalyticsResp;
import com.qykj.finmaster.analytics.oa.service.OaAnalyticsService;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * OA 分析
 * 费用/薪资相关直接复用已有的 /oa/report/expense、/oa/report/payroll 接口
 * 这里新增考勤合规率分析
 */
@RestController
@RequestMapping("/analytics/oa")
public class OaAnalyticsController extends BaseController {

    @Autowired
    private OaAnalyticsService service;

    @GetMapping("/attendance-compliance")
    public AjaxResult attendanceCompliance(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.attendanceCompliance(getLoginUser(), req));
    }

    /** 请假/加班分析 */
    @GetMapping("/leave-overtime")
    public AjaxResult leaveOvertime(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(service.leaveOvertime(getLoginUser(), req));
    }

    @GetMapping("/export/compliance")
    public void exportCompliance(@ModelAttribute AnalyticsQueryReq req, HttpServletResponse response) {
        OaAnalyticsResp.AttendanceComplianceResp resp = service.attendanceCompliance(getLoginUser(), req);
        List<OaAnalyticsResp.UserCompliance> list = resp.getWorstComplianceUsers() == null
                ? Collections.emptyList() : resp.getWorstComplianceUsers();
        new ExcelUtil<>(OaAnalyticsResp.UserCompliance.class).exportExcel(response, list, "考勤合规明细");
    }
}
