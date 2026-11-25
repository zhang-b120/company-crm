package com.qykj.finmaster.analytics.sales.service;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.common.context.AnalyticsContextBuilder;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.analytics.common.enums.AnalyticsScope;
import com.qykj.finmaster.analytics.common.util.TrendBuilder;
import com.qykj.finmaster.analytics.sales.dto.SalesPerformanceResp;
import com.qykj.finmaster.analytics.sales.mapper.SalesAnalyticsMapper;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 三层级业绩聚合：PERSONAL / TEAM / COMPANY 用同一接口返回
 */
@Service
public class SalesAnalyticsService {

    @Autowired
    private AnalyticsContextBuilder contextBuilder;

    @Autowired
    private SalesAnalyticsMapper mapper;

    public SalesPerformanceResp performance(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();

        SalesPerformanceResp resp = new SalesPerformanceResp();
        resp.setScope(ctx.getScope().name());

        // ─── KPI ────────────────────────────────────────────────────────────
        SalesPerformanceResp.Kpi kpi = new SalesPerformanceResp.Kpi();
        BigDecimal amount = nvl(mapper.sumOrderAmount(ctx, s, e));
        Long count = nvlLong(mapper.countOrders(ctx, s, e));
        Long deal = nvlLong(mapper.countDealCustomers(ctx, s, e));
        kpi.setOrderAmount(amount);
        kpi.setOrderCount(count);
        kpi.setDealCustomerCount(deal);
        kpi.setAvgOrderAmount(count > 0
                ? amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        if (ctx.hasCompare()) {
            BigDecimal prevAmount = nvl(mapper.sumOrderAmount(ctx, ctx.getPrevTimeStart(), ctx.getPrevTimeEnd()));
            Long prevCount = nvlLong(mapper.countOrders(ctx, ctx.getPrevTimeStart(), ctx.getPrevTimeEnd()));
            Long prevDeal = nvlLong(mapper.countDealCustomers(ctx, ctx.getPrevTimeStart(), ctx.getPrevTimeEnd()));
            kpi.setOrderAmountGrowth(TrendBuilder.growthPercent(amount, prevAmount));
            kpi.setOrderCountGrowth(TrendBuilder.growthPercent(count, prevCount));
            kpi.setDealCustomerCountGrowth(TrendBuilder.growthPercent(deal, prevDeal));
        }
        resp.setKpi(kpi);

        // ─── 趋势 ───────────────────────────────────────────────────────────
        TrendBuilder.Granularity g = TrendBuilder.pickGranularity(s, e);
        List<Map<String, Object>> rows = g == TrendBuilder.Granularity.DAY
                ? mapper.orderTrendByDay(ctx, s, e)
                : mapper.orderTrendByMonth(ctx, s, e);
        Map<String, BigDecimal> valueMap = new HashMap<>();
        Map<String, Long> countMap = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String label = String.valueOf(r.get("label"));
                valueMap.put(label, toBig(r.get("value")));
                countMap.put(label, toLong(r.get("count")));
            }
        }
        List<TrendPointResp> trend = TrendBuilder.fill(s, e, g, valueMap, countMap);
        resp.setTrend(trend);

        // ─── 排行榜（非 PERSONAL） ──────────────────────────────────────────
        if (ctx.getScope() != AnalyticsScope.PERSONAL) {
            List<SalesPerformanceResp.RankItem> ranking = mapper.salesRanking(ctx, s, e);
            if (ranking == null) ranking = new ArrayList<>();

            // 计算每人环比 + 贡献率
            BigDecimal total = amount;
            for (SalesPerformanceResp.RankItem it : ranking) {
                if (it.getOrderAmount() == null) it.setOrderAmount(BigDecimal.ZERO);
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    it.setContributionRate(it.getOrderAmount()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(total, 2, RoundingMode.HALF_UP));
                } else {
                    it.setContributionRate(BigDecimal.ZERO);
                }
                if (ctx.hasCompare() && it.getUsername() != null) {
                    BigDecimal prev = nvl(mapper.salesAmountOfUser(
                            ctx.getTenantId(),
                            it.getUsername(),
                            ctx.getPrevTimeStart(),
                            ctx.getPrevTimeEnd()));
                    it.setGrowth(TrendBuilder.growthPercent(it.getOrderAmount(), prev));
                }
            }
            resp.setRanking(ranking);
        } else {
            resp.setRanking(Collections.emptyList());
        }

        // ─── 部门维度（仅 COMPANY） ─────────────────────────────────────────
        if (ctx.getScope() == AnalyticsScope.COMPANY) {
            List<SalesPerformanceResp.DeptStat> ds = mapper.deptStats(ctx, s, e);
            if (ds == null) ds = new ArrayList<>();
            BigDecimal total = amount;
            for (SalesPerformanceResp.DeptStat d : ds) {
                if (d.getOrderAmount() == null) d.setOrderAmount(BigDecimal.ZERO);
                if (total.compareTo(BigDecimal.ZERO) > 0) {
                    d.setContributionRate(d.getOrderAmount()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(total, 2, RoundingMode.HALF_UP));
                } else {
                    d.setContributionRate(BigDecimal.ZERO);
                }
            }
            resp.setDeptStats(ds);
        } else {
            resp.setDeptStats(Collections.emptyList());
        }

        return resp;
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static Long nvlLong(Long v) { return v == null ? 0L : v; }

    private static Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception ex) { return 0L; }
    }

    private static BigDecimal toBig(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return BigDecimal.valueOf(((Number) o).doubleValue());
        try { return new BigDecimal(o.toString()); } catch (Exception ex) { return BigDecimal.ZERO; }
    }
}
