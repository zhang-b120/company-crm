package com.qykj.finmaster.workbench.controller;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.workbench.service.BossWorkbenchService;
import com.qykj.finmaster.workbench.service.CsLeaderService;
import com.qykj.finmaster.workbench.service.CsWorkOrderOverviewService;
import com.qykj.finmaster.workbench.service.CsWorkbenchService;
import com.qykj.finmaster.workbench.service.SalesLeaderService;
import com.qykj.finmaster.workbench.service.SalesWorkbenchService;
import com.qykj.finmaster.workbench.service.WorkbenchConfigService;
import com.qykj.finmaster.workbench.service.WorkbenchService;
import com.qykj.finmaster.workbench.service.WorkbenchTodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页工作台接口
 * - GET /workbench/config                       可见工作台列表 + 各工作台可用层级
 * - GET /workbench/market/summary               市场部工作台聚合数据
 * - GET /workbench/sales/summary                销售部工作台聚合数据
 * - GET /workbench/cs/summary                   客服部工作台聚合数据
 */
@RestController
@RequestMapping("/workbench")
public class WorkbenchController extends BaseController {

    @Autowired
    private WorkbenchConfigService configService;

    @Autowired
    private WorkbenchService workbenchService;

    @Autowired
    private BossWorkbenchService bossWorkbenchService;

    @Autowired
    private SalesWorkbenchService salesWorkbenchService;

    @Autowired
    private WorkbenchTodoService todoService;

    @Autowired
    private CsWorkbenchService csWorkbenchService;

    @Autowired
    private SalesLeaderService salesLeaderService;

    @Autowired
    private CsLeaderService csLeaderService;

    @Autowired
    private CsWorkOrderOverviewService csWorkOrderOverviewService;

    /**
     * 客服部工单总览（工单 74）：按客服人员的工单状态分布 + 超时/挂起监控。
     * 范围固定客服部（code=KFB），纯只读统计。
     */
    @GetMapping("/cs/work-order-overview")
    public AjaxResult csWorkOrderOverview(
            @org.springframework.web.bind.annotation.RequestParam(value = "taskType", required = false) String taskType,
            @org.springframework.web.bind.annotation.RequestParam(value = "month", required = false) String month) {
        return AjaxResult.success(csWorkOrderOverviewService.overview(getLoginUser(), taskType, month));
    }

    /** 客服部工单总览下钻明细（工单 74 / CSWO-03）：按 bucket + 可选 username/taskType/month 分页。 */
    @GetMapping("/cs/work-order-overview/drill")
    public AjaxResult csWorkOrderOverviewDrill(
            @org.springframework.web.bind.annotation.RequestParam("bucket") String bucket,
            @org.springframework.web.bind.annotation.RequestParam(value = "username", required = false) String username,
            @org.springframework.web.bind.annotation.RequestParam(value = "taskType", required = false) String taskType,
            @org.springframework.web.bind.annotation.RequestParam(value = "month", required = false) String month,
            @org.springframework.web.bind.annotation.RequestParam(value = "pageNum", required = false, defaultValue = "1") Integer pageNum,
            @org.springframework.web.bind.annotation.RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        return AjaxResult.success(csWorkOrderOverviewService.drill(getLoginUser(), bucket, username, taskType, month, pageNum, pageSize));
    }

    @GetMapping("/config")
    public AjaxResult config() {
        return AjaxResult.success(configService.getConfig(getLoginUser()));
    }

    @GetMapping("/market/summary")
    public AjaxResult marketSummary(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(workbenchService.marketSummary(getLoginUser(), req));
    }

    @GetMapping("/sales/summary")
    public AjaxResult salesSummary(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(workbenchService.salesSummary(getLoginUser(), req));
    }

    @GetMapping("/cs/summary")
    public AjaxResult csSummary(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(workbenchService.csSummary(getLoginUser(), req));
    }

    @GetMapping("/boss/summary")
    public AjaxResult bossSummary(@ModelAttribute AnalyticsQueryReq req) {
        return AjaxResult.success(bossWorkbenchService.getSummary(getLoginUser(), req));
    }

    /**
     * 销售工作台个性化数据（支持 scope=PERSONAL/TEAM）
     */
    @GetMapping("/sales/personal-summary")
    public AjaxResult salesPersonalSummary(
            @org.springframework.web.bind.annotation.RequestParam(value = "scope", required = false) String scope,
            @org.springframework.web.bind.annotation.RequestParam(value = "signPeriod", required = false) String signPeriod,
            @org.springframework.web.bind.annotation.RequestParam(value = "signStart", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate signStart,
            @org.springframework.web.bind.annotation.RequestParam(value = "signEnd", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate signEnd,
            @org.springframework.web.bind.annotation.RequestParam(value = "leadPeriod", required = false) String leadPeriod,
            @org.springframework.web.bind.annotation.RequestParam(value = "leadStart", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate leadStart,
            @org.springframework.web.bind.annotation.RequestParam(value = "leadEnd", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate leadEnd,
            @org.springframework.web.bind.annotation.RequestParam(value = "customerPeriod", required = false) String customerPeriod,
            @org.springframework.web.bind.annotation.RequestParam(value = "customerStart", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate customerStart,
            @org.springframework.web.bind.annotation.RequestParam(value = "customerEnd", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate customerEnd) {
        SalesWorkbenchService.SummaryReq req = new SalesWorkbenchService.SummaryReq();
        req.setScope(parseSalesScope(scope));
        req.setSignPeriod(parsePeriod(signPeriod, SalesWorkbenchService.PeriodType.TODAY));
        req.setSignStart(signStart);
        req.setSignEnd(signEnd);
        req.setLeadPeriod(parsePeriod(leadPeriod, SalesWorkbenchService.PeriodType.MONTH));
        req.setLeadStart(leadStart);
        req.setLeadEnd(leadEnd);
        req.setCustomerPeriod(parsePeriod(customerPeriod, SalesWorkbenchService.PeriodType.MONTH));
        req.setCustomerStart(customerStart);
        req.setCustomerEnd(customerEnd);
        return AjaxResult.success(salesWorkbenchService.summary(getLoginUser(), req));
    }

    private SalesWorkbenchService.PeriodType parsePeriod(String s, SalesWorkbenchService.PeriodType fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try { return SalesWorkbenchService.PeriodType.valueOf(s.toUpperCase()); }
        catch (Exception e) { return fallback; }
    }
    private SalesWorkbenchService.Scope parseSalesScope(String s) {
        if (s == null || s.isEmpty()) return SalesWorkbenchService.Scope.PERSONAL;
        try { return SalesWorkbenchService.Scope.valueOf(s.toUpperCase()); }
        catch (Exception e) { return SalesWorkbenchService.Scope.PERSONAL; }
    }
    private CsWorkbenchService.Scope parseCsScope(String s) {
        if (s == null || s.isEmpty()) return CsWorkbenchService.Scope.PERSONAL;
        try { return CsWorkbenchService.Scope.valueOf(s.toUpperCase()); }
        catch (Exception e) { return CsWorkbenchService.Scope.PERSONAL; }
    }

    /** 销售明细：签单 / 线索 / 客户 by member */
    @GetMapping("/sales/drill")
    public AjaxResult salesDrill(
            @org.springframework.web.bind.annotation.RequestParam("metric") String metric,
            @org.springframework.web.bind.annotation.RequestParam(value = "period", required = false) String period,
            @org.springframework.web.bind.annotation.RequestParam(value = "start", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @org.springframework.web.bind.annotation.RequestParam(value = "end", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        SalesWorkbenchService.PeriodType type = parsePeriod(period, SalesWorkbenchService.PeriodType.MONTH);
        switch (metric.toLowerCase()) {
            case "sign":
                return AjaxResult.success(salesWorkbenchService.drillSign(getLoginUser(), type, start, end));
            case "lead":
                return AjaxResult.success(salesWorkbenchService.drillLead(getLoginUser(), type, start, end));
            case "customer":
                return AjaxResult.success(salesWorkbenchService.drillCustomer(getLoginUser(), type, start, end));
            default:
                return AjaxResult.error("未知的明细 metric: " + metric);
        }
    }

    /** 客服明细：bookkeeping / renewal / dataCollection / fieldWork by member */
    @GetMapping("/cs/drill")
    public AjaxResult csDrill(
            @org.springframework.web.bind.annotation.RequestParam("metric") String metric,
            @org.springframework.web.bind.annotation.RequestParam(value = "period", required = false) String period) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.util.Date monthStart = java.util.Date.from(
                today.withDayOfMonth(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date monthEnd = java.util.Date.from(
                today.withDayOfMonth(1).plusMonths(1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        java.util.Date now = new java.util.Date();
        switch (metric.toLowerCase()) {
            case "bookkeeping":
                return AjaxResult.success(csWorkbenchService.drillBookkeeping(getLoginUser(), monthStart, monthEnd));
            case "renewal":
                return AjaxResult.success(csWorkbenchService.drillRenewal(getLoginUser(), monthStart, monthEnd, now));
            case "datacollection":
                return AjaxResult.success(csWorkbenchService.drillDataCollection(getLoginUser(), period));
            case "fieldwork":
                return AjaxResult.success(csWorkbenchService.drillFieldWork(getLoginUser(), monthStart, monthEnd));
            default:
                return AjaxResult.error("未知的明细 metric: " + metric);
        }
    }

    /** 我的待办（右下角面板）：work_order + 今日待跟进线索 + 今日待跟进客户 */
    @GetMapping("/todo/my")
    public AjaxResult myTodo() {
        return AjaxResult.success(todoService.getMyTodo(getLoginUser()));
    }

    /**
     * 客服工作台个性化数据（支持 scope=PERSONAL/TEAM）
     */
    @GetMapping("/cs/personal-summary")
    public AjaxResult csPersonalSummary(
            @org.springframework.web.bind.annotation.RequestParam(value = "scope", required = false) String scope,
            @org.springframework.web.bind.annotation.RequestParam(value = "period", required = false) String period) {
        return AjaxResult.success(csWorkbenchService.summary(getLoginUser(), parseCsScope(scope), period));
    }

    /** 销售团队长工作台 */
    @GetMapping("/sales/leader-summary")
    public AjaxResult salesLeaderSummary() {
        return AjaxResult.success(salesLeaderService.summary(getLoginUser()));
    }

    /** 客服团队长工作台 */
    @GetMapping("/cs/leader-summary")
    public AjaxResult csLeaderSummary() {
        return AjaxResult.success(csLeaderService.summary(getLoginUser()));
    }

    /** 销售团队长 A1/A2 弹窗明细懒加载（type=todayPending|aboutRecycle）*/
    @GetMapping("/sales/leader-detail")
    public AjaxResult salesLeaderDetail(
            @org.springframework.web.bind.annotation.RequestParam("type") String type) {
        return AjaxResult.success(salesLeaderService.detailList(getLoginUser(), type));
    }

    /** 销售员工诊断画像 */
    @GetMapping("/sales/diagnose")
    public AjaxResult salesDiagnose(
            @org.springframework.web.bind.annotation.RequestParam("username") String username) {
        return AjaxResult.success(salesLeaderService.diagnose(getLoginUser(), username));
    }

    /**
     * 销售团队漏斗（按线索创建时间，独立时间筛选）
     * periodType: CURR_MONTH / LAST_MONTH / CUSTOM
     * CUSTOM 时配 start/end (yyyy-MM-dd)
     */
    @GetMapping("/sales/leader-funnel")
    public AjaxResult salesLeaderFunnel(
            @org.springframework.web.bind.annotation.RequestParam(value = "periodType", required = false) String periodType,
            @org.springframework.web.bind.annotation.RequestParam(value = "start", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @org.springframework.web.bind.annotation.RequestParam(value = "end", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime s, e;
        String pt = periodType == null ? "CURR_MONTH" : periodType.toUpperCase();
        switch (pt) {
            case "LAST_MONTH": {
                java.time.LocalDate ms = today.withDayOfMonth(1).minusMonths(1);
                s = ms.atStartOfDay();
                e = today.withDayOfMonth(1).atStartOfDay();
                break;
            }
            case "CUSTOM": {
                java.time.LocalDate ss = start != null ? start : today.withDayOfMonth(1);
                java.time.LocalDate ee = end != null ? end : today;
                s = ss.atStartOfDay();
                e = ee.plusDays(1).atStartOfDay();
                break;
            }
            case "CURR_MONTH":
            default: {
                java.time.LocalDate ms = today.withDayOfMonth(1);
                s = ms.atStartOfDay();
                e = ms.plusMonths(1).atStartOfDay();
            }
        }
        return AjaxResult.success(salesLeaderService.funnelByPeriod(getLoginUser(), s, e));
    }

    /**
     * 销售团队漏斗按成员明细（同 leader-funnel 时间窗参数）
     */
    @GetMapping("/sales/leader-funnel-by-member")
    public AjaxResult salesLeaderFunnelByMember(
            @org.springframework.web.bind.annotation.RequestParam(value = "periodType", required = false) String periodType,
            @org.springframework.web.bind.annotation.RequestParam(value = "start", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate start,
            @org.springframework.web.bind.annotation.RequestParam(value = "end", required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate end) {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDateTime s, e;
        String pt = periodType == null ? "CURR_MONTH" : periodType.toUpperCase();
        switch (pt) {
            case "LAST_MONTH": {
                java.time.LocalDate ms = today.withDayOfMonth(1).minusMonths(1);
                s = ms.atStartOfDay();
                e = today.withDayOfMonth(1).atStartOfDay();
                break;
            }
            case "CUSTOM": {
                java.time.LocalDate ss = start != null ? start : today.withDayOfMonth(1);
                java.time.LocalDate ee = end != null ? end : today;
                s = ss.atStartOfDay();
                e = ee.plusDays(1).atStartOfDay();
                break;
            }
            case "CURR_MONTH":
            default: {
                java.time.LocalDate ms = today.withDayOfMonth(1);
                s = ms.atStartOfDay();
                e = ms.plusMonths(1).atStartOfDay();
            }
        }
        return AjaxResult.success(salesLeaderService.funnelByMember(getLoginUser(), s, e));
    }

    /** 客服管家诊断画像 */
    @GetMapping("/cs/diagnose")
    public AjaxResult csDiagnose(
            @org.springframework.web.bind.annotation.RequestParam("username") String username) {
        return AjaxResult.success(csLeaderService.diagnose(getLoginUser(), username));
    }
}
