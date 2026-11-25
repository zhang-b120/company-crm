package com.qykj.finmaster.workbench.service;

import com.qykj.finmaster.analytics.call.dto.CallAnalyticsResp;
import com.qykj.finmaster.analytics.call.service.CallAnalyticsService;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.cs.dto.CsAnalyticsResp;
import com.qykj.finmaster.analytics.cs.service.CsAnalyticsService;
import com.qykj.finmaster.analytics.customer.dto.CustomerAnalyticsResp;
import com.qykj.finmaster.analytics.customer.service.CustomerAnalyticsService;
import com.qykj.finmaster.analytics.fieldwork.dto.FieldWorkAnalyticsResp;
import com.qykj.finmaster.analytics.fieldwork.service.FieldWorkAnalyticsService;
import com.qykj.finmaster.analytics.lead.dto.LeadAnalyticsResp;
import com.qykj.finmaster.analytics.lead.service.LeadAnalyticsService;
import com.qykj.finmaster.analytics.order.dto.OrderAnalyticsResp;
import com.qykj.finmaster.analytics.order.service.OrderAnalyticsService;
import com.qykj.finmaster.analytics.sales.dto.SalesPerformanceResp;
import com.qykj.finmaster.analytics.sales.service.SalesAnalyticsService;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.workbench.dto.WorkbenchSummaryResp;
import com.qykj.finmaster.workbench.enums.WorkbenchType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 工作台首页聚合 Service
 * 各工作台复用已有 analytics Service 获取数据，选取最关键的几个 KPI + 排行 + 饼图拼装成统一结构
 * workbenchType 会被写入 req 并透传至 AnalyticsContextBuilder，用于按工作台类型收缩 TEAM 权限
 */
@Service
public class WorkbenchService {

    @Autowired
    private LeadAnalyticsService leadService;

    @Autowired
    private CustomerAnalyticsService customerService;

    @Autowired
    private SalesAnalyticsService salesService;

    @Autowired
    private OrderAnalyticsService orderService;

    @Autowired
    private CallAnalyticsService callService;

    @Autowired
    private CsAnalyticsService csService;

    @Autowired
    private FieldWorkAnalyticsService fieldWorkService;

    // ─── 市场部工作台 ───────────────────────────────────────────────────────
    public WorkbenchSummaryResp marketSummary(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsQueryReq r = withType(req, WorkbenchType.MARKET.getCode());
        WorkbenchSummaryResp resp = new WorkbenchSummaryResp();
        resp.setType(WorkbenchType.MARKET.getCode());

        // 主体：线索漏斗 + 跟进时效
        LeadAnalyticsResp.FunnelResp f = leadService.funnel(user, r);
        LeadAnalyticsResp.FollowUpTimingResp t = leadService.followUpTiming(user, r);
        resp.setScope(f.getScope());

        addKpi(resp, "totalLeads", "线索总数", f.getTotalLeads(), null, "normal", null);
        addKpi(resp, "unfollowed", "待跟进",
                f.getUnfollowed(),
                f.getUnfollowed() != null && f.getUnfollowed() > 0 ? "需尽快分配/跟进" : null,
                f.getUnfollowed() != null && f.getUnfollowed() > 0 ? "warning" : "normal", null);
        addKpi(resp, "converted", "已转化", f.getConverted(), null, "normal", null);
        addKpiPercent(resp, "conversionRate", "转化率", f.getConversionRate(), null, "normal");
        addKpi(resp, "invalid", "已失效",
                f.getInvalid(), null,
                f.getInvalid() != null && f.getInvalid() > 0 ? "warning" : "normal", null);
        if (t != null) {
            addKpiDecimal(resp, "avgFirstResponseHours", "平均首次响应",
                    t.getAvgFirstResponseHours(), "小时", "normal");
        }

        // 来源 TOP 5（饼图）
        List<LeadAnalyticsResp.SourceRoiItem> roi = leadService.sourceRoi(user, r);
        if (roi != null) {
            int cap = Math.min(roi.size(), 5);
            for (int i = 0; i < cap; i++) {
                LeadAnalyticsResp.SourceRoiItem it = roi.get(i);
                WorkbenchSummaryResp.PieItem p = new WorkbenchSummaryResp.PieItem();
                p.setName(it.getSourceLabel() != null ? it.getSourceLabel() : it.getSource());
                p.setValue(it.getLeadCount());
                resp.getPie().add(p);
            }
            // 转化率排行前 5
            int rank = 0;
            for (LeadAnalyticsResp.SourceRoiItem it : roi) {
                if (rank >= 5) break;
                WorkbenchSummaryResp.RankItem ri = new WorkbenchSummaryResp.RankItem();
                ri.setRank(++rank);
                ri.setName(it.getSourceLabel() != null ? it.getSourceLabel() : it.getSource());
                ri.setValue(it.getConversionRate());
                ri.setSub(it.getLeadCount() + " 条 · 转化 " + nvl(it.getConvertedCount()));
                resp.getRanking().add(ri);
            }
        }
        return resp;
    }

    // ─── 销售部工作台 ───────────────────────────────────────────────────────
    public WorkbenchSummaryResp salesSummary(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsQueryReq r = withType(req, WorkbenchType.SALES.getCode());
        WorkbenchSummaryResp resp = new WorkbenchSummaryResp();
        resp.setType(WorkbenchType.SALES.getCode());

        // 业绩
        SalesPerformanceResp perf = salesService.performance(user, r);
        resp.setScope(perf.getScope());
        if (perf.getKpi() != null) {
            SalesPerformanceResp.Kpi k = perf.getKpi();
            addKpiAmount(resp, "orderAmount", "订单金额", k.getOrderAmount(), k.getOrderAmountGrowth(), "normal");
            addKpi(resp, "orderCount", "订单数", k.getOrderCount(),
                    null, "normal", k.getOrderCountGrowth());
            addKpiAmount(resp, "avgOrderAmount", "客单价", k.getAvgOrderAmount(), null, "normal");
            addKpi(resp, "dealCustomerCount", "成交客户", k.getDealCustomerCount(),
                    null, "normal", k.getDealCustomerCountGrowth());
        }

        // 私海
        CustomerAnalyticsResp.PrivateSeaResp ps = customerService.privateSea(user, r, null);
        if (ps != null) {
            addKpi(resp, "myPrivateSea", "私海客户", ps.getTotalCount(), null, "normal", null);
            addKpi(resp, "neverFollowed", "未跟进客户",
                    ps.getNeverFollowedCount(),
                    ps.getNeverFollowedCount() != null && ps.getNeverFollowedCount() > 0 ? "需尽快跟进" : null,
                    ps.getNeverFollowedCount() != null && ps.getNeverFollowedCount() > 0 ? "warning" : "normal",
                    null);
        }

        // 通话
        CallAnalyticsResp.OverviewResp call = callService.overview(user, r);
        if (call != null) {
            addKpi(resp, "totalCalls", "通话数", call.getTotalCalls(), null, "normal", null);
            addKpiPercent(resp, "connectedRate", "接通率", call.getConnectedRate(), null, "normal");
        }

        // 退款预警
        OrderAnalyticsResp.RefundResp refund = orderService.refund(user, r);
        if (refund != null) {
            addKpiAmountNegative(resp, "refundAmount", "退款金额",
                    refund.getRefundAmount(), refund.getRefundAmountGrowth());
            addKpiPercent(resp, "refundRate", "退款率",
                    refund.getRefundRate(), null,
                    refund.getRefundRate() != null && refund.getRefundRate().compareTo(BigDecimal.TEN) > 0
                            ? "danger" : "normal");
        }

        // 销售排行
        if (perf.getRanking() != null) {
            int rank = 0;
            for (SalesPerformanceResp.RankItem it : perf.getRanking()) {
                if (rank >= 5) break;
                WorkbenchSummaryResp.RankItem ri = new WorkbenchSummaryResp.RankItem();
                ri.setRank(++rank);
                ri.setName(it.getRealName() != null ? it.getRealName() : it.getUsername());
                ri.setValue(it.getOrderAmount());
                ri.setSub(nvl(it.getOrderCount()) + " 单 · 贡献 " + nvlPercent(it.getContributionRate()) + "%");
                resp.getRanking().add(ri);
            }
        }
        return resp;
    }

    // ─── 客服部工作台 ───────────────────────────────────────────────────────
    public WorkbenchSummaryResp csSummary(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsQueryReq r = withType(req, WorkbenchType.CS.getCode());
        WorkbenchSummaryResp resp = new WorkbenchSummaryResp();
        resp.setType(WorkbenchType.CS.getCode());

        // 续费
        CsAnalyticsResp.RenewalResp renewal = csService.renewal(user, r);
        resp.setScope(renewal.getScope());
        addKpi(resp, "expiringCount", "到期客户", renewal.getExpiringCount(), null, "normal", null);
        addKpi(resp, "renewedCount", "已续费", renewal.getRenewedCount(), null, "normal", null);
        addKpi(resp, "notRenewedCount", "未续费",
                renewal.getNotRenewedCount(),
                renewal.getNotRenewedCount() != null && renewal.getNotRenewedCount() > 0 ? "未续费占比高" : null,
                renewal.getNotRenewedCount() != null && renewal.getNotRenewedCount() > 0 ? "warning" : "normal",
                null);
        addKpiPercent(resp, "renewalRate", "续费率", renewal.getRenewalRate(), null, "normal");
        addKpiAmount(resp, "renewAmount", "续费金额", renewal.getRenewAmount(), null, "normal");

        // 任务单
        CsAnalyticsResp.TaskSummaryResp task = csService.taskSummary(user, r);
        if (task != null) {
            addKpi(resp, "taskInProgress", "处理中任务", task.getInProgressTasks(), null, "normal", null);
            addKpi(resp, "taskOverdue", "超期任务",
                    task.getOverdueTasks(),
                    task.getOverdueTasks() != null && task.getOverdueTasks() > 0 ? "需要立刻处理" : null,
                    task.getOverdueTasks() != null && task.getOverdueTasks() > 0 ? "danger" : "normal",
                    null);
            addKpiPercent(resp, "completionRate", "完成率", task.getCompletionRate(), null, "normal");
        }

        // 外勤
        FieldWorkAnalyticsResp.OverviewResp field = fieldWorkService.overview(user, r);
        if (field != null) {
            addKpi(resp, "fieldPending", "待执行外勤", field.getPendingTasks(), null, "normal", null);
            addKpi(resp, "fieldInProgress", "进行中外勤", field.getInProgressTasks(), null, "normal", null);
        }

        // 未续费原因 TOP 5（饼图）
        CsAnalyticsResp.NotRenewReasonsResp reasons = csService.notRenewReasons(user, r);
        if (reasons != null && reasons.getReasons() != null) {
            int cap = Math.min(reasons.getReasons().size(), 5);
            for (int i = 0; i < cap; i++) {
                CsAnalyticsResp.StringKeyCount sk = reasons.getReasons().get(i);
                WorkbenchSummaryResp.PieItem p = new WorkbenchSummaryResp.PieItem();
                p.setName(sk.getName() != null ? sk.getName() : "未填写");
                p.setValue(sk.getCount());
                resp.getPie().add(p);
            }
        }

        // 管家续费排行 TOP 5
        if (renewal.getButlerRanking() != null) {
            int rank = 0;
            for (CsAnalyticsResp.ButlerRankItem br : renewal.getButlerRanking()) {
                if (rank >= 5) break;
                WorkbenchSummaryResp.RankItem ri = new WorkbenchSummaryResp.RankItem();
                ri.setRank(++rank);
                ri.setName(br.getButlerUserName() != null ? br.getButlerUserName() : br.getButlerUser());
                ri.setValue(br.getRenewalRate());
                ri.setSub("到期 " + nvl(br.getExpiringCount()) + " · 续费 " + nvl(br.getRenewedCount()));
                resp.getRanking().add(ri);
            }
        }
        return resp;
    }

    // ─── 辅助方法 ─────────────────────────────────────────────────────────
    private AnalyticsQueryReq withType(AnalyticsQueryReq req, String type) {
        AnalyticsQueryReq r = req == null ? new AnalyticsQueryReq() : req;
        r.setWorkbenchType(type);
        return r;
    }

    private void addKpi(WorkbenchSummaryResp resp, String key, String label,
                        Object value, String sub, String tone, BigDecimal growth) {
        WorkbenchSummaryResp.KpiCard c = new WorkbenchSummaryResp.KpiCard();
        c.setKey(key);
        c.setLabel(label);
        c.setValue(value == null ? 0 : value);
        c.setSub(sub);
        c.setTone(tone);
        c.setGrowth(growth);
        resp.getKpis().add(c);
    }

    private void addKpiDecimal(WorkbenchSummaryResp resp, String key, String label,
                               BigDecimal value, String unit, String tone) {
        WorkbenchSummaryResp.KpiCard c = new WorkbenchSummaryResp.KpiCard();
        c.setKey(key);
        c.setLabel(label);
        c.setValue(value == null ? BigDecimal.ZERO : value);
        c.setSub(unit);
        c.setTone(tone);
        resp.getKpis().add(c);
    }

    private void addKpiAmount(WorkbenchSummaryResp resp, String key, String label,
                              BigDecimal value, BigDecimal growth, String tone) {
        WorkbenchSummaryResp.KpiCard c = new WorkbenchSummaryResp.KpiCard();
        c.setKey(key);
        c.setLabel(label);
        c.setValue(value == null ? BigDecimal.ZERO : value);
        c.setSub("元");
        c.setGrowth(growth);
        c.setTone(tone);
        resp.getKpis().add(c);
    }

    private void addKpiAmountNegative(WorkbenchSummaryResp resp, String key, String label,
                                      BigDecimal value, BigDecimal growth) {
        WorkbenchSummaryResp.KpiCard c = new WorkbenchSummaryResp.KpiCard();
        c.setKey(key);
        c.setLabel(label);
        c.setValue(value == null ? BigDecimal.ZERO : value);
        c.setSub("元");
        c.setGrowth(growth);
        c.setNegativeIsGood(Boolean.TRUE);
        c.setTone(value != null && value.compareTo(BigDecimal.ZERO) > 0 ? "warning" : "normal");
        resp.getKpis().add(c);
    }

    private void addKpiPercent(WorkbenchSummaryResp resp, String key, String label,
                               BigDecimal value, BigDecimal growth, String tone) {
        WorkbenchSummaryResp.KpiCard c = new WorkbenchSummaryResp.KpiCard();
        c.setKey(key);
        c.setLabel(label);
        c.setValue(value == null ? BigDecimal.ZERO : value);
        c.setSub("%");
        c.setGrowth(growth);
        c.setTone(tone);
        resp.getKpis().add(c);
    }

    private static Long nvl(Long v) { return v == null ? 0L : v; }
    private static BigDecimal nvlPercent(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
