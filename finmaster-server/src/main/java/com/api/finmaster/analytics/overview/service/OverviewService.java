package com.qykj.finmaster.analytics.overview.service;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.common.context.AnalyticsContextBuilder;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.analytics.common.service.AnalyticsScopeResolver;
import com.qykj.finmaster.analytics.common.util.TrendBuilder;
import com.qykj.finmaster.analytics.overview.dto.OverviewResp;
import com.qykj.finmaster.analytics.overview.mapper.OverviewMapper;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 综合看板：顶部 KPI + 线索漏斗 + 订单趋势 + 销售 TOP5
 * 返回的 KPI 自带同环比增长率（依据 req.compareWith）
 */
@Service
public class OverviewService {

    @Autowired
    private AnalyticsContextBuilder contextBuilder;

    @Autowired
    private AnalyticsScopeResolver scopeResolver;

    @Autowired
    private OverviewMapper overviewMapper;

    public OverviewResp getOverview(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);

        OverviewResp resp = new OverviewResp();
        resp.setScope(ctx.getScope().name());
        resp.setAvailableScopes(scopeResolver.availableScopes(user));

        // ─── KPI ────────────────────────────────────────────────────────────
        OverviewResp.KpiBlock kpi = new OverviewResp.KpiBlock();
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();
        kpi.setNewLeads(nvl(overviewMapper.countNewLeads(ctx, s, e)));
        kpi.setNewCustomers(nvl(overviewMapper.countNewCustomers(ctx, s, e)));
        kpi.setOrderAmount(nvlAmount(overviewMapper.sumOrderAmount(ctx, s, e)));
        kpi.setCallCount(nvl(overviewMapper.countCalls(ctx, s, e)));

        if (ctx.hasCompare()) {
            LocalDateTime ps = ctx.getPrevTimeStart(), pe = ctx.getPrevTimeEnd();
            Long prevLeads = nvl(overviewMapper.countNewLeads(ctx, ps, pe));
            Long prevCustomers = nvl(overviewMapper.countNewCustomers(ctx, ps, pe));
            BigDecimal prevOrderAmount = nvlAmount(overviewMapper.sumOrderAmount(ctx, ps, pe));
            Long prevCalls = nvl(overviewMapper.countCalls(ctx, ps, pe));
            kpi.setNewLeadsGrowth(TrendBuilder.growthPercent(kpi.getNewLeads(), prevLeads));
            kpi.setNewCustomersGrowth(TrendBuilder.growthPercent(kpi.getNewCustomers(), prevCustomers));
            kpi.setOrderAmountGrowth(TrendBuilder.growthPercent(kpi.getOrderAmount(), prevOrderAmount));
            kpi.setCallCountGrowth(TrendBuilder.growthPercent(kpi.getCallCount(), prevCalls));
        }
        resp.setKpis(kpi);

        // ─── 线索漏斗 ───────────────────────────────────────────────────────
        List<Map<String, Object>> funnelRows = overviewMapper.leadFunnel(ctx, s, e);
        resp.setLeadFunnel(toFunnel(funnelRows));

        // ─── 订单趋势 ───────────────────────────────────────────────────────
        TrendBuilder.Granularity g = TrendBuilder.pickGranularity(s, e);
        List<Map<String, Object>> trendRows = g == TrendBuilder.Granularity.DAY
                ? overviewMapper.orderTrendByDay(ctx, s, e)
                : overviewMapper.orderTrendByMonth(ctx, s, e);
        Map<String, BigDecimal> valueMap = new HashMap<>();
        Map<String, Long> countMap = new HashMap<>();
        if (trendRows != null) {
            for (Map<String, Object> row : trendRows) {
                String label = toStr(row.get("label"));
                if (label == null) continue;
                valueMap.put(label, toBigDecimal(row.get("value")));
                countMap.put(label, toLong(row.get("count")));
            }
        }
        List<TrendPointResp> trend = TrendBuilder.fill(s, e, g, valueMap, countMap);
        resp.setOrderTrend(trend);

        // ─── 销售 TOP5 ──────────────────────────────────────────────────────
        List<OverviewResp.SalesTopItem> tops = overviewMapper.salesTopN(ctx, s, e, 5);
        resp.setSalesTop5(tops == null ? new ArrayList<>() : tops);

        return resp;
    }

    // ─── 辅助 ────────────────────────────────────────────────────────────────
    private static Long nvl(Long v) { return v == null ? 0L : v; }

    private static BigDecimal nvlAmount(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static List<OverviewResp.FunnelItem> toFunnel(List<Map<String, Object>> rows) {
        Map<Integer, Long> counts = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                Integer status = toInt(r.get("status"));
                Long c = toLong(r.get("count"));
                if (status != null) counts.put(status, c == null ? 0L : c);
            }
        }
        String[] labels = {"未跟进", "跟进中", "已转化", "已失效"};
        List<OverviewResp.FunnelItem> out = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            OverviewResp.FunnelItem item = new OverviewResp.FunnelItem();
            item.setStatus(i);
            item.setStatusLabel(labels[i - 1]);
            item.setCount(counts.getOrDefault(i, 0L));
            out.add(item);
        }
        return out;
    }

    private static String toStr(Object o) { return o == null ? null : o.toString(); }

    private static Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(o.toString());
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception ex) { return null; }
    }

    private static BigDecimal toBigDecimal(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return BigDecimal.valueOf(((Number) o).doubleValue());
        return new BigDecimal(o.toString());
    }

    // 用于解释 funnel 状态的 helper（暴露给外部测试需要）
    public static List<Integer> knownFunnelStatuses() {
        return Arrays.asList(1, 2, 3, 4);
    }
}
