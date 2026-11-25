package com.qykj.finmaster.analytics.customer.service;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.common.context.AnalyticsContextBuilder;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.analytics.common.util.TrendBuilder;
import com.qykj.finmaster.analytics.customer.dto.CustomerAnalyticsResp;
import com.qykj.finmaster.analytics.customer.mapper.CustomerAnalyticsMapper;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerAnalyticsService {

    /** 沉睡阈值（天），可后续抽到配置表 */
    private static final int DEFAULT_DORMANT_DAYS = 30;

    private static final String[] STATUS_LABELS = {"未跟进", "跟进中", "已转化", "已失效"};
    private static final String STATUS_NULL_LABEL = "未设置";

    /** 生命周期分桶边界（单位：天） */
    private static final int[] CYCLE_BUCKETS = {3, 7, 15, 30, 60, 90};
    private static final String[] CYCLE_LABELS = {"≤3天", "3-7天", "7-15天", "15-30天", "30-60天", "60-90天", "90天+"};

    @Autowired
    private AnalyticsContextBuilder contextBuilder;

    @Autowired
    private CustomerAnalyticsMapper mapper;

    public CustomerAnalyticsResp.PrivateSeaResp privateSea(LoginUser user, AnalyticsQueryReq req, Integer dormantDays) {
        // dormantDays 参数保留兼容旧调用，当前口径不再按时间判定沉睡
        AnalyticsContext ctx = contextBuilder.build(user, req);

        CustomerAnalyticsResp.PrivateSeaResp resp = new CustomerAnalyticsResp.PrivateSeaResp();
        resp.setScope(ctx.getScope().name());

        Long total = nvl(mapper.countPrivateSea(ctx));
        Long dormant = nvl(mapper.countDormant(ctx));
        Long neverFollowed = nvl(mapper.countNeverFollowed(ctx));
        Long deal = nvl(mapper.countDealCustomers(ctx));
        resp.setTotalCount(total);
        resp.setDormantCount(dormant);
        resp.setNeverFollowedCount(neverFollowed);
        resp.setDealRate(total > 0
                ? BigDecimal.valueOf(deal).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // status 分布
        List<Map<String, Object>> rows = mapper.statusDistribution(ctx);
        Map<Integer, Long> m = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                Integer status = toInt(r.get("status"));
                Long c = toLong(r.get("count"));
                if (status != null) m.put(status, c == null ? 0L : c);
            }
        }
        List<CustomerAnalyticsResp.StatusItem> statusItems = new ArrayList<>();
        // 0 = 未设置（SQL 已用 COALESCE 把 NULL 归到 0）；1-4 = 业务状态
        Long nullCount = m.getOrDefault(0, 0L);
        if (nullCount > 0) {
            CustomerAnalyticsResp.StatusItem it = new CustomerAnalyticsResp.StatusItem();
            it.setStatus(0);
            it.setStatusLabel(STATUS_NULL_LABEL);
            it.setCount(nullCount);
            statusItems.add(it);
        }
        for (int i = 1; i <= 4; i++) {
            CustomerAnalyticsResp.StatusItem it = new CustomerAnalyticsResp.StatusItem();
            it.setStatus(i);
            it.setStatusLabel(STATUS_LABELS[i - 1]);
            it.setCount(m.getOrDefault(i, 0L));
            statusItems.add(it);
        }
        resp.setStatusDist(statusItems);

        // owner load（私海客户的负责人负载 + 从未跟进占比）
        List<CustomerAnalyticsResp.OwnerLoad> loads = mapper.ownerLoad(ctx);
        resp.setOwnerLoad(loads == null ? new ArrayList<>() : loads);
        return resp;
    }

    public CustomerAnalyticsResp.LifecycleResp lifecycle(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);

        CustomerAnalyticsResp.LifecycleResp resp = new CustomerAnalyticsResp.LifecycleResp();

        // 线索→首单
        List<BigDecimal> leadToOrder = mapper.leadToOrderDays(ctx);
        resp.setLeadToOrder(bucketize(leadToOrder));
        resp.setAvgLeadToOrderDays(average(leadToOrder));

        // 首单→续费
        List<BigDecimal> orderToRenew = mapper.orderToRenewDays(ctx);
        resp.setOrderToRenew(bucketize(orderToRenew));
        resp.setAvgOrderToRenewDays(average(orderToRenew));

        // 漏斗
        CustomerAnalyticsResp.StageFunnel sf = new CustomerAnalyticsResp.StageFunnel();
        sf.setTotalLeads(nvl(mapper.countTotalLeads(ctx)));
        sf.setConvertedCustomers(nvl(mapper.countConvertedCustomers(ctx)));
        sf.setFirstOrderCustomers(nvl(mapper.countFirstOrderCustomers(ctx)));
        sf.setRenewedCustomers(nvl(mapper.countRenewedCustomers(ctx)));
        resp.setStageFunnel(sf);
        return resp;
    }

    public CustomerAnalyticsResp.PublicSeaResp publicSea(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();

        CustomerAnalyticsResp.PublicSeaResp resp = new CustomerAnalyticsResp.PublicSeaResp();
        resp.setScope(ctx.getScope().name());
        Long transferIn = nvl(mapper.countTransferIn(ctx, s, e));
        Long claim = nvl(mapper.countClaim(ctx, s, e));
        Long assign = nvl(mapper.countAssign(ctx, s, e));
        resp.setTransferInCount(transferIn);
        resp.setClaimCount(claim);
        resp.setAssignCount(assign);
        long recycled = claim + assign;
        resp.setRecycleRate(transferIn > 0
                ? BigDecimal.valueOf(recycled).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(transferIn), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        resp.setAvgStayDays(nvlB(mapper.avgStayDays(ctx, s, e)));
        resp.setCurrentPublicSeaCount(nvl(mapper.countCurrentPublicSea(ctx)));

        // 趋势
        TrendBuilder.Granularity g = TrendBuilder.pickGranularity(s, e);
        resp.setTransferTrend(buildCountTrend(
                g == TrendBuilder.Granularity.DAY
                        ? mapper.transferTrendByDay(ctx, s, e)
                        : mapper.transferTrendByMonth(ctx, s, e),
                s, e, g));
        resp.setRecycleTrend(buildCountTrend(
                g == TrendBuilder.Granularity.DAY
                        ? mapper.recycleTrendByDay(ctx, s, e)
                        : mapper.recycleTrendByMonth(ctx, s, e),
                s, e, g));

        List<CustomerAnalyticsResp.ReleaseItem> rel = mapper.topReleasers(ctx, s, e);
        resp.setTopReleasers(rel == null ? new ArrayList<>() : rel);
        return resp;
    }

    private static List<TrendPointResp> buildCountTrend(List<Map<String, Object>> rows,
                                                        LocalDateTime start, LocalDateTime end,
                                                        TrendBuilder.Granularity g) {
        Map<String, Long> cm = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                cm.put(String.valueOf(r.get("label")), toLong(r.get("count")));
            }
        }
        return TrendBuilder.fill(start, end, g, new HashMap<>(), cm);
    }

    private static BigDecimal nvlB(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private static List<CustomerAnalyticsResp.CycleBucket> bucketize(List<BigDecimal> days) {
        long[] counts = new long[CYCLE_LABELS.length];
        if (days != null) {
            for (BigDecimal d : days) {
                if (d == null) continue;
                double v = d.doubleValue();
                int idx = CYCLE_BUCKETS.length;
                for (int i = 0; i < CYCLE_BUCKETS.length; i++) {
                    if (v <= CYCLE_BUCKETS[i]) { idx = i; break; }
                }
                counts[idx]++;
            }
        }
        List<CustomerAnalyticsResp.CycleBucket> out = new ArrayList<>();
        for (int i = 0; i < CYCLE_LABELS.length; i++) {
            CustomerAnalyticsResp.CycleBucket b = new CustomerAnalyticsResp.CycleBucket();
            b.setLabel(CYCLE_LABELS[i]);
            b.setCount(counts[i]);
            out.add(b);
        }
        return out;
    }

    private static BigDecimal average(List<BigDecimal> vals) {
        if (vals == null || vals.isEmpty()) return BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        long cnt = 0;
        for (BigDecimal v : vals) {
            if (v == null) continue;
            sum = sum.add(v);
            cnt++;
        }
        if (cnt == 0) return BigDecimal.ZERO;
        return sum.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP);
    }

    private static Long nvl(Long v) { return v == null ? 0L : v; }
    private static Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception ex) { return 0L; }
    }
    private static Integer toInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception ex) { return null; }
    }
}
