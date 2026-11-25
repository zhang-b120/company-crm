package com.qykj.finmaster.analytics.call.service;

import com.qykj.finmaster.analytics.call.dto.CallAnalyticsResp;
import com.qykj.finmaster.analytics.call.mapper.CallAnalyticsMapper;
import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.common.context.AnalyticsContextBuilder;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.analytics.common.util.TrendBuilder;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CallAnalyticsService {

    @Autowired
    private AnalyticsContextBuilder contextBuilder;

    @Autowired
    private CallAnalyticsMapper mapper;

    public CallAnalyticsResp.OverviewResp overview(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();

        CallAnalyticsResp.OverviewResp resp = new CallAnalyticsResp.OverviewResp();
        resp.setScope(ctx.getScope().name());
        resp.setTotalCalls(nvlLong(mapper.countTotal(ctx, s, e)));
        resp.setConnected(nvlLong(mapper.countConnected(ctx, s, e)));
        resp.setNotConnected(nvlLong(mapper.countNotConnected(ctx, s, e)));

        long base = resp.getConnected() + resp.getNotConnected();
        resp.setConnectedRate(base > 0
                ? BigDecimal.valueOf(resp.getConnected())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(base), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        resp.setTotalDurationSec(nvlLong(mapper.sumDuration(ctx, s, e)));
        resp.setAvgDurationSec(nvl(mapper.avgDuration(ctx, s, e)));

        Long qd = nvlLong(mapper.countQualityDone(ctx, s, e));
        resp.setQualityCoverageRate(resp.getConnected() > 0
                ? BigDecimal.valueOf(qd).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(resp.getConnected()), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        resp.setAvgQualityScore(nvl(mapper.avgQualityScore(ctx, s, e)));

        // 趋势
        TrendBuilder.Granularity g = TrendBuilder.pickGranularity(s, e);
        List<Map<String, Object>> rows = g == TrendBuilder.Granularity.DAY
                ? mapper.trendByDay(ctx, s, e)
                : mapper.trendByMonth(ctx, s, e);
        Map<String, Long> cm = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                cm.put(String.valueOf(r.get("label")), toLong(r.get("count")));
            }
        }
        List<TrendPointResp> trend = TrendBuilder.fill(s, e, g, new HashMap<>(), cm);
        resp.setCallTrend(trend);
        return resp;
    }

    public List<CallAnalyticsResp.RankingItem> ranking(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        List<CallAnalyticsResp.RankingItem> list = mapper.ranking(ctx, ctx.getTimeStart(), ctx.getTimeEnd());
        return list == null ? new ArrayList<>() : list;
    }

    public List<CallAnalyticsResp.LowScoreCall> lowScoreCalls(LoginUser user, AnalyticsQueryReq req, Integer limit) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        int l = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        List<CallAnalyticsResp.LowScoreCall> list = mapper.lowScoreCalls(ctx, ctx.getTimeStart(), ctx.getTimeEnd(), l);
        return list == null ? new ArrayList<>() : list;
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static Long nvlLong(Long v) { return v == null ? 0L : v; }
    private static Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception ex) { return 0L; }
    }
}
