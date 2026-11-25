package com.qykj.finmaster.analytics.lead.service;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.common.context.AnalyticsContextBuilder;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.analytics.common.util.TrendBuilder;
import com.qykj.finmaster.analytics.lead.dto.LeadAnalyticsResp;
import com.qykj.finmaster.analytics.lead.mapper.LeadAnalyticsMapper;
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
public class LeadAnalyticsService {

    private static final Map<String, String> SOURCE_LABELS = new HashMap<>();
    static {
        SOURCE_LABELS.put("1", "小程序注册");
        SOURCE_LABELS.put("small_produce", "小程序注册");
        SOURCE_LABELS.put("2", "引流");
        SOURCE_LABELS.put("3", "抖音");
        SOURCE_LABELS.put("4", "小红书");
        SOURCE_LABELS.put("5", "官网");
        SOURCE_LABELS.put("6", "客户介绍");
        SOURCE_LABELS.put("UNKNOWN", "未知来源");
    }

    private static final List<String> BUCKET_ORDER =
            Arrays.asList("<1h", "1-4h", "4-24h", "1-3d", "3-7d", "7d+");

    @Autowired
    private AnalyticsContextBuilder contextBuilder;

    @Autowired
    private LeadAnalyticsMapper mapper;

    public LeadAnalyticsResp.FunnelResp funnel(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        List<Map<String, Object>> rows = mapper.funnelCounts(ctx, ctx.getTimeStart(), ctx.getTimeEnd());
        Map<Integer, Long> byStatus = new HashMap<>();
        long total = 0L;
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                Integer s = toInt(r.get("status"));
                Long c = toLong(r.get("count"));
                if (s != null) {
                    byStatus.put(s, c == null ? 0L : c);
                    total += c == null ? 0L : c;
                }
            }
        }
        LeadAnalyticsResp.FunnelResp resp = new LeadAnalyticsResp.FunnelResp();
        resp.setScope(ctx.getScope().name());
        resp.setTotalLeads(total);
        resp.setUnfollowed(byStatus.getOrDefault(1, 0L));
        resp.setFollowing(byStatus.getOrDefault(2, 0L));
        resp.setConverted(byStatus.getOrDefault(3, 0L));
        resp.setInvalid(byStatus.getOrDefault(4, 0L));
        if (total > 0) {
            resp.setConversionRate(BigDecimal.valueOf(resp.getConverted())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
            resp.setValidRate(BigDecimal.valueOf(total - resp.getInvalid())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP));
        } else {
            resp.setConversionRate(BigDecimal.ZERO);
            resp.setValidRate(BigDecimal.ZERO);
        }
        return resp;
    }

    public List<LeadAnalyticsResp.SourceRoiItem> sourceRoi(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        List<LeadAnalyticsResp.SourceRoiItem> items = mapper.sourceRoi(ctx, ctx.getTimeStart(), ctx.getTimeEnd());
        if (items == null) return new ArrayList<>();
        for (LeadAnalyticsResp.SourceRoiItem it : items) {
            it.setSourceLabel(SOURCE_LABELS.getOrDefault(it.getSource(), it.getSource()));
            if (it.getOrderAmount() == null) it.setOrderAmount(BigDecimal.ZERO);
        }
        return items;
    }

    public LeadAnalyticsResp.FollowUpTimingResp followUpTiming(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();

        LeadAnalyticsResp.FollowUpTimingResp resp = new LeadAnalyticsResp.FollowUpTimingResp();
        resp.setAvgFirstResponseHours(nvl(mapper.avgFirstResponseHours(ctx, s, e)));
        resp.setAvgConversionDays(nvl(mapper.avgConversionDays(ctx, s, e)));
        resp.setAvgFollowUpCount(nvl(mapper.avgFollowUpCount(ctx, s, e)));

        Map<String, Long> bucketMap = new HashMap<>();
        List<Map<String, Object>> rows = mapper.firstResponseBuckets(ctx, s, e);
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String label = String.valueOf(r.get("label"));
                Long c = toLong(r.get("count"));
                bucketMap.put(label, c == null ? 0L : c);
            }
        }
        List<LeadAnalyticsResp.BucketItem> buckets = new ArrayList<>();
        for (String name : BUCKET_ORDER) {
            LeadAnalyticsResp.BucketItem it = new LeadAnalyticsResp.BucketItem();
            it.setLabel(name);
            it.setCount(bucketMap.getOrDefault(name, 0L));
            buckets.add(it);
        }
        resp.setFirstResponseBuckets(buckets);
        return resp;
    }

    public LeadAnalyticsResp.TrendResp trend(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();
        TrendBuilder.Granularity g = TrendBuilder.pickGranularity(s, e);

        List<Map<String, Object>> newRows = g == TrendBuilder.Granularity.DAY
                ? mapper.newLeadsTrendByDay(ctx, s, e)
                : mapper.newLeadsTrendByMonth(ctx, s, e);
        List<Map<String, Object>> convRows = g == TrendBuilder.Granularity.DAY
                ? mapper.convertedTrendByDay(ctx, s, e)
                : mapper.convertedTrendByMonth(ctx, s, e);

        List<TrendPointResp> newPoints = TrendBuilder.fill(s, e, g,
                new HashMap<>(), toCountMap(newRows));
        List<TrendPointResp> convPoints = TrendBuilder.fill(s, e, g,
                new HashMap<>(), toCountMap(convRows));

        LeadAnalyticsResp.TrendResp resp = new LeadAnalyticsResp.TrendResp();
        resp.setNewLeads(newPoints);
        resp.setConverted(convPoints);

        if (ctx.hasCompare()) {
            Long curNew = nvlLong(mapper.countTotal(ctx, s, e));
            Long prevNew = nvlLong(mapper.countTotal(ctx, ctx.getPrevTimeStart(), ctx.getPrevTimeEnd()));
            Long curConv = nvlLong(mapper.countConverted(ctx, s, e));
            Long prevConv = nvlLong(mapper.countConverted(ctx, ctx.getPrevTimeStart(), ctx.getPrevTimeEnd()));
            resp.setNewLeadsGrowth(TrendBuilder.growthPercent(curNew, prevNew));
            resp.setConvertedGrowth(TrendBuilder.growthPercent(curConv, prevConv));
        }
        return resp;
    }

    private static Map<String, Long> toCountMap(List<Map<String, Object>> rows) {
        Map<String, Long> m = new HashMap<>();
        if (rows == null) return m;
        for (Map<String, Object> r : rows) {
            String label = String.valueOf(r.get("label"));
            Long c = toLong(r.get("count"));
            m.put(label, c == null ? 0L : c);
        }
        return m;
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static Long nvlLong(Long v) { return v == null ? 0L : v; }

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
