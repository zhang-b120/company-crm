package com.qykj.finmaster.workbench.service;

import com.qykj.finmaster.analytics.call.dto.CallAnalyticsResp;
import com.qykj.finmaster.analytics.call.service.CallAnalyticsService;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.cs.dto.CsAnalyticsResp;
import com.qykj.finmaster.analytics.cs.service.CsAnalyticsService;
import com.qykj.finmaster.analytics.lead.dto.LeadAnalyticsResp;
import com.qykj.finmaster.analytics.lead.service.LeadAnalyticsService;
import com.qykj.finmaster.analytics.order.dto.OrderAnalyticsResp;
import com.qykj.finmaster.analytics.order.service.OrderAnalyticsService;
import com.qykj.finmaster.analytics.overview.dto.OverviewResp;
import com.qykj.finmaster.analytics.overview.service.OverviewService;
import com.qykj.finmaster.analytics.sales.dto.SalesPerformanceResp;
import com.qykj.finmaster.analytics.sales.service.SalesAnalyticsService;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.workbench.dto.BossWorkbenchResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 老板经营看板：六板块聚合
 * 强制 scope=COMPANY；底层权限由 AnalyticsScopeResolver cap
 * 所有指标复用现有 analytics Service，未新增 SQL
 */
@Service
public class BossWorkbenchService {

    @Autowired
    private OverviewService overviewService;

    @Autowired
    private LeadAnalyticsService leadService;

    @Autowired
    private SalesAnalyticsService salesService;

    @Autowired
    private OrderAnalyticsService orderService;

    @Autowired
    private CallAnalyticsService callService;

    @Autowired
    private CsAnalyticsService csService;

    public BossWorkbenchResp getSummary(LoginUser user, AnalyticsQueryReq req) {
        BossWorkbenchResp resp = new BossWorkbenchResp();
        AnalyticsQueryReq r = forceCompany(req);

        // 一次性拿到多个 analytics 返回
        OverviewResp ov = overviewService.getOverview(user, r);
        LeadAnalyticsResp.FunnelResp lf = leadService.funnel(user, r);
        List<LeadAnalyticsResp.SourceRoiItem> lroi = leadService.sourceRoi(user, r);
        LeadAnalyticsResp.TrendResp ltrend = leadService.trend(user, r);
        SalesPerformanceResp sp = salesService.performance(user, r);
        OrderAnalyticsResp.SummaryResp os = orderService.summary(user, r);
        OrderAnalyticsResp.RefundResp oref = orderService.refund(user, r);
        CallAnalyticsResp.OverviewResp co = callService.overview(user, r);
        CsAnalyticsResp.RenewalResp cr = csService.renewal(user, r);
        CsAnalyticsResp.TaskSummaryResp ct = csService.taskSummary(user, r);

        resp.setScope(ov.getScope());

        // ─── ① 核心 KPI ────────────────────────────────────────────
        addKpi(resp, "revenue", "本期营收",
                os.getKpi() != null ? os.getKpi().getOrderAmount() : BigDecimal.ZERO,
                os.getKpi() != null ? os.getKpi().getOrderAmountGrowth() : null,
                "money", false);
        addKpi(resp, "orderCount", "订单数",
                os.getKpi() != null ? os.getKpi().getOrderCount() : 0L,
                os.getKpi() != null ? os.getKpi().getOrderCountGrowth() : null,
                "count", false);
        if (ov.getKpis() != null) {
            addKpi(resp, "newLeads", "新增线索",
                    ov.getKpis().getNewLeads(),
                    ov.getKpis().getNewLeadsGrowth(),
                    "count", false);
            addKpi(resp, "newCustomers", "新增客户",
                    ov.getKpis().getNewCustomers(),
                    ov.getKpis().getNewCustomersGrowth(),
                    "count", false);
            addKpi(resp, "callCount", "通话数",
                    ov.getKpis().getCallCount(),
                    ov.getKpis().getCallCountGrowth(),
                    "count", false);
        }
        addKpi(resp, "refundAmount", "退款金额",
                oref.getRefundAmount(),
                oref.getRefundAmountGrowth(),
                "money", true);

        // ─── ② 部门对比 ────────────────────────────────────────────
        resp.setMarketDept(buildMarketCard(lf, lroi));
        resp.setSalesDept(buildSalesCard(sp, co));
        resp.setCsDept(buildCsCard(cr, ct));

        // ─── ③ 经营趋势 ────────────────────────────────────────────
        if (ov.getOrderTrend() != null) {
            resp.setRevenueTrend(ov.getOrderTrend());
        }
        // 新增客户趋势用 lead.trend.converted（线索转化为客户）作为近似曲线
        if (ltrend != null && ltrend.getConverted() != null) {
            resp.setCustomerTrend(ltrend.getConverted());
        }

        // ─── ④ 健康度 ──────────────────────────────────────────────
        resp.getHealthIndicators().add(health("conversionRate", "线索转化率",
                lf.getConversionRate(), false,
                new BigDecimal("15"), new BigDecimal("5"),
                "目标 ≥ 15%"));
        resp.getHealthIndicators().add(health("connectedRate", "通话接通率",
                co.getConnectedRate(), false,
                new BigDecimal("70"), new BigDecimal("50"),
                "目标 ≥ 70%"));
        resp.getHealthIndicators().add(health("renewalRate", "续费率",
                cr.getRenewalRate(), false,
                new BigDecimal("85"), new BigDecimal("70"),
                "目标 ≥ 85%"));
        resp.getHealthIndicators().add(healthReverse("refundRate", "退款率",
                oref.getRefundRate(),
                new BigDecimal("5"), new BigDecimal("10"),
                "目标 ≤ 5%"));
        resp.getHealthIndicators().add(health("taskCompletionRate", "任务完成率",
                ct != null ? ct.getCompletionRate() : BigDecimal.ZERO, false,
                new BigDecimal("80"), new BigDecimal("80"),
                "目标 ≥ 80%"));

        // ─── ⑤ 人员排行 TOP 10 ─────────────────────────────────────
        if (sp.getRanking() != null) {
            int rank = 0;
            for (SalesPerformanceResp.RankItem it : sp.getRanking()) {
                if (rank >= 10) break;
                BossWorkbenchResp.SalesRankItem si = new BossWorkbenchResp.SalesRankItem();
                si.setRank(++rank);
                si.setUsername(it.getUsername());
                si.setRealName(it.getRealName());
                si.setOrderCount(it.getOrderCount());
                si.setOrderAmount(it.getOrderAmount());
                si.setContributionRate(it.getContributionRate());
                si.setGrowth(it.getGrowth());
                resp.getSalesTop().add(si);
            }
        }
        if (cr.getButlerRanking() != null) {
            int rank = 0;
            for (CsAnalyticsResp.ButlerRankItem it : cr.getButlerRanking()) {
                if (rank >= 10) break;
                BossWorkbenchResp.ButlerRankItem bi = new BossWorkbenchResp.ButlerRankItem();
                bi.setRank(++rank);
                bi.setUsername(it.getButlerUser());
                bi.setRealName(it.getButlerUserName());
                bi.setExpiringCount(it.getExpiringCount());
                bi.setRenewedCount(it.getRenewedCount());
                bi.setRenewalRate(it.getRenewalRate());
                bi.setRenewAmount(it.getRenewAmount());
                resp.getButlerTop().add(bi);
            }
        }

        // ─── ⑥ 结构分析 ────────────────────────────────────────────
        if (lroi != null) {
            int cap = Math.min(lroi.size(), 8);
            for (int i = 0; i < cap; i++) {
                LeadAnalyticsResp.SourceRoiItem it = lroi.get(i);
                BossWorkbenchResp.PieItem p = new BossWorkbenchResp.PieItem();
                p.setName(it.getSourceLabel() != null ? it.getSourceLabel() : it.getSource());
                p.setValue(it.getLeadCount());
                resp.getLeadSourcePie().add(p);
            }
        }
        if (os.getProductMix() != null) {
            for (OrderAnalyticsResp.ProductMixItem it : os.getProductMix()) {
                BossWorkbenchResp.PieItem p = new BossWorkbenchResp.PieItem();
                p.setName(it.getProductName() != null ? it.getProductName() : "其他");
                p.setValue(it.getAmount());
                resp.getProductMixPie().add(p);
            }
        }
        return resp;
    }

    // ─── 部门卡组装 ──────────────────────────────────────────────────

    private BossWorkbenchResp.DeptCard buildMarketCard(
            LeadAnalyticsResp.FunnelResp lf,
            List<LeadAnalyticsResp.SourceRoiItem> lroi) {
        BossWorkbenchResp.DeptCard c = new BossWorkbenchResp.DeptCard();
        c.setDeptType("MARKET");
        c.setLabel("市场部");
        addDeptKpi(c, "新增线索", nvlLong(lf.getTotalLeads()), "count");
        addDeptKpi(c, "转化数", nvlLong(lf.getConverted()), "count");
        addDeptKpi(c, "转化率", nvl(lf.getConversionRate()), "percent");
        addDeptKpi(c, "待跟进", nvlLong(lf.getUnfollowed()), "count");
        // TOP 来源（文本形式放在 label 里展示）
        if (lroi != null && !lroi.isEmpty()) {
            LeadAnalyticsResp.SourceRoiItem top = lroi.get(0);
            BossWorkbenchResp.DeptKpi k = new BossWorkbenchResp.DeptKpi();
            k.setLabel("TOP 来源");
            k.setValue(top.getSourceLabel() != null ? top.getSourceLabel() : top.getSource());
            k.setUnit("text");
            c.getItems().add(k);
        }
        return c;
    }

    private BossWorkbenchResp.DeptCard buildSalesCard(
            SalesPerformanceResp sp,
            CallAnalyticsResp.OverviewResp co) {
        BossWorkbenchResp.DeptCard c = new BossWorkbenchResp.DeptCard();
        c.setDeptType("SALES");
        c.setLabel("销售部");
        if (sp.getKpi() != null) {
            addDeptKpi(c, "签约金额", nvl(sp.getKpi().getOrderAmount()), "money");
            addDeptKpi(c, "订单数", nvlLong(sp.getKpi().getOrderCount()), "count");
            addDeptKpi(c, "客单价", nvl(sp.getKpi().getAvgOrderAmount()), "money");
            addDeptKpi(c, "成交客户", nvlLong(sp.getKpi().getDealCustomerCount()), "count");
        }
        if (co != null) {
            addDeptKpi(c, "通话数", nvlLong(co.getTotalCalls()), "count");
        }
        return c;
    }

    private BossWorkbenchResp.DeptCard buildCsCard(
            CsAnalyticsResp.RenewalResp cr,
            CsAnalyticsResp.TaskSummaryResp ct) {
        BossWorkbenchResp.DeptCard c = new BossWorkbenchResp.DeptCard();
        c.setDeptType("CS");
        c.setLabel("客服部");
        addDeptKpi(c, "到期客户", nvlLong(cr.getExpiringCount()), "count");
        addDeptKpi(c, "已续费", nvlLong(cr.getRenewedCount()), "count");
        addDeptKpi(c, "续费率", nvl(cr.getRenewalRate()), "percent");
        addDeptKpi(c, "续费金额", nvl(cr.getRenewAmount()), "money");
        if (ct != null) {
            addDeptKpi(c, "任务完成", nvlLong(ct.getCompletedTasks()), "count");
        }
        return c;
    }

    // ─── 健康度构造 ──────────────────────────────────────────────────

    private BossWorkbenchResp.HealthItem health(String key, String label,
                                                BigDecimal rate, boolean negativeIsGood,
                                                BigDecimal greenMin, BigDecimal yellowMin,
                                                String threshold) {
        BossWorkbenchResp.HealthItem h = new BossWorkbenchResp.HealthItem();
        h.setKey(key);
        h.setLabel(label);
        h.setRate(nvl(rate));
        h.setNegativeIsGood(negativeIsGood);
        h.setThreshold(threshold);
        BigDecimal r = nvl(rate);
        if (r.compareTo(greenMin) >= 0) {
            h.setTone("normal");
        } else if (r.compareTo(yellowMin) >= 0) {
            h.setTone("warning");
        } else {
            h.setTone("danger");
        }
        return h;
    }

    /** 反向指标（越低越好，如退款率）：rate ≤ greenMax 绿；rate ≤ redMin 黄；> redMin 红 */
    private BossWorkbenchResp.HealthItem healthReverse(String key, String label,
                                                       BigDecimal rate,
                                                       BigDecimal greenMax, BigDecimal redMin,
                                                       String threshold) {
        BossWorkbenchResp.HealthItem h = new BossWorkbenchResp.HealthItem();
        h.setKey(key);
        h.setLabel(label);
        h.setRate(nvl(rate));
        h.setNegativeIsGood(Boolean.TRUE);
        h.setThreshold(threshold);
        BigDecimal r = nvl(rate);
        if (r.compareTo(greenMax) <= 0) {
            h.setTone("normal");
        } else if (r.compareTo(redMin) <= 0) {
            h.setTone("warning");
        } else {
            h.setTone("danger");
        }
        return h;
    }

    // ─── 辅助 ────────────────────────────────────────────────────────

    private AnalyticsQueryReq forceCompany(AnalyticsQueryReq req) {
        AnalyticsQueryReq r = req == null ? new AnalyticsQueryReq() : req;
        r.setScope("COMPANY");
        r.setWorkbenchType(null);
        return r;
    }

    private void addKpi(BossWorkbenchResp resp, String key, String label,
                        Object value, BigDecimal growth, String unit, boolean negativeIsGood) {
        BossWorkbenchResp.KpiCard c = new BossWorkbenchResp.KpiCard();
        c.setKey(key);
        c.setLabel(label);
        c.setValue(value == null ? 0 : value);
        c.setGrowth(growth);
        c.setUnit(unit);
        c.setNegativeIsGood(negativeIsGood);
        resp.getCoreKpis().add(c);
    }

    private void addDeptKpi(BossWorkbenchResp.DeptCard c, String label, Object value, String unit) {
        BossWorkbenchResp.DeptKpi k = new BossWorkbenchResp.DeptKpi();
        k.setLabel(label);
        k.setValue(value);
        k.setUnit(unit);
        c.getItems().add(k);
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static Long nvlLong(Long v) { return v == null ? 0L : v; }
}
