package com.qykj.finmaster.analytics.order.service;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.common.context.AnalyticsContextBuilder;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.analytics.common.util.TrendBuilder;
import com.qykj.finmaster.analytics.order.dto.OrderAnalyticsResp;
import com.qykj.finmaster.analytics.order.mapper.OrderAnalyticsMapper;
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
public class OrderAnalyticsService {

    private static final Map<String, String> DETAIL_TYPE_LABELS = new HashMap<>();
    static {
        DETAIL_TYPE_LABELS.put("NEW", "新增");
        DETAIL_TYPE_LABELS.put("RENEW", "续费");
        DETAIL_TYPE_LABELS.put("BALANCE", "补款");
        DETAIL_TYPE_LABELS.put("OTHER", "其他");
    }

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

    @Autowired
    private AnalyticsContextBuilder contextBuilder;

    @Autowired
    private OrderAnalyticsMapper mapper;

    public OrderAnalyticsResp.SummaryResp summary(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();

        OrderAnalyticsResp.SummaryResp resp = new OrderAnalyticsResp.SummaryResp();
        resp.setScope(ctx.getScope().name());

        OrderAnalyticsResp.Kpi kpi = new OrderAnalyticsResp.Kpi();
        BigDecimal amount = nvl(mapper.sumAmount(ctx, s, e));
        Long count = nvlLong(mapper.countOrders(ctx, s, e));
        Long deal = nvlLong(mapper.countDealCustomers(ctx, s, e));
        kpi.setOrderAmount(amount);
        kpi.setOrderCount(count);
        kpi.setDealCustomerCount(deal);
        kpi.setAvgOrderAmount(count > 0
                ? amount.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        if (ctx.hasCompare()) {
            BigDecimal prevAmount = nvl(mapper.sumAmount(ctx, ctx.getPrevTimeStart(), ctx.getPrevTimeEnd()));
            Long prevCount = nvlLong(mapper.countOrders(ctx, ctx.getPrevTimeStart(), ctx.getPrevTimeEnd()));
            kpi.setOrderAmountGrowth(TrendBuilder.growthPercent(amount, prevAmount));
            kpi.setOrderCountGrowth(TrendBuilder.growthPercent(count, prevCount));
        }
        resp.setKpi(kpi);

        // 趋势
        TrendBuilder.Granularity g = TrendBuilder.pickGranularity(s, e);
        List<Map<String, Object>> rows = g == TrendBuilder.Granularity.DAY
                ? mapper.trendByDay(ctx, s, e)
                : mapper.trendByMonth(ctx, s, e);
        Map<String, BigDecimal> vm = new HashMap<>();
        Map<String, Long> cm = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String label = String.valueOf(r.get("label"));
                vm.put(label, toBig(r.get("value")));
                cm.put(label, toLong(r.get("count")));
            }
        }
        List<TrendPointResp> trend = TrendBuilder.fill(s, e, g, vm, cm);
        resp.setTrend(trend);

        // 产品结构（计算占比）
        List<OrderAnalyticsResp.ProductMixItem> pm = mapper.productMix(ctx, s, e);
        if (pm == null) pm = new ArrayList<>();
        BigDecimal pmTotal = pm.stream()
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (OrderAnalyticsResp.ProductMixItem p : pm) {
            if (p.getAmount() == null) p.setAmount(BigDecimal.ZERO);
            p.setAmountRate(pmTotal.compareTo(BigDecimal.ZERO) > 0
                    ? p.getAmount().multiply(BigDecimal.valueOf(100))
                        .divide(pmTotal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
        }
        resp.setProductMix(pm);

        // 销售归因（计算占比）
        List<OrderAnalyticsResp.SalesAttribution> sa = mapper.salesAttribution(ctx, s, e);
        if (sa == null) sa = new ArrayList<>();
        for (OrderAnalyticsResp.SalesAttribution a : sa) {
            if (a.getAmount() == null) a.setAmount(BigDecimal.ZERO);
            a.setAmountRate(amount.compareTo(BigDecimal.ZERO) > 0
                    ? a.getAmount().multiply(BigDecimal.valueOf(100))
                        .divide(amount, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
        }
        resp.setSalesAttribution(sa);

        // 明细类型分布
        List<OrderAnalyticsResp.DetailTypeItem> dt = mapper.detailTypeDist(ctx, s, e);
        if (dt == null) dt = new ArrayList<>();
        for (OrderAnalyticsResp.DetailTypeItem i : dt) {
            i.setTypeLabel(DETAIL_TYPE_LABELS.getOrDefault(i.getType(), i.getType()));
            if (i.getAmount() == null) i.setAmount(BigDecimal.ZERO);
        }
        resp.setDetailTypeDist(dt);

        return resp;
    }

    public OrderAnalyticsResp.RefundResp refund(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();

        OrderAnalyticsResp.RefundResp resp = new OrderAnalyticsResp.RefundResp();
        resp.setScope(ctx.getScope().name());

        BigDecimal normal = nvl(mapper.sumNormalAmount(ctx, s, e));
        Long normalCount = nvlLong(mapper.countNormalOrders(ctx, s, e));
        BigDecimal refund = nvl(mapper.sumRefundAmount(ctx, s, e));
        Long refundCount = nvlLong(mapper.countRefundOrders(ctx, s, e));
        resp.setNormalOrderAmount(normal);
        resp.setNormalOrderCount(normalCount);
        resp.setRefundAmount(refund);
        resp.setRefundOrderCount(refundCount);
        resp.setRefundRate(normal.compareTo(BigDecimal.ZERO) > 0
                ? refund.multiply(BigDecimal.valueOf(100)).divide(normal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        if (ctx.hasCompare()) {
            BigDecimal prevRefund = nvl(mapper.sumRefundAmount(ctx, ctx.getPrevTimeStart(), ctx.getPrevTimeEnd()));
            resp.setRefundAmountGrowth(TrendBuilder.growthPercent(refund, prevRefund));
        }

        // 趋势
        TrendBuilder.Granularity g = TrendBuilder.pickGranularity(s, e);
        List<Map<String, Object>> rows = g == TrendBuilder.Granularity.DAY
                ? mapper.refundTrendByDay(ctx, s, e)
                : mapper.refundTrendByMonth(ctx, s, e);
        Map<String, BigDecimal> vm = new HashMap<>();
        Map<String, Long> cm = new HashMap<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String label = String.valueOf(r.get("label"));
                vm.put(label, toBig(r.get("value")));
                cm.put(label, toLong(r.get("count")));
            }
        }
        resp.setRefundTrend(TrendBuilder.fill(s, e, g, vm, cm));

        // 责任人排行
        List<OrderAnalyticsResp.RefundOwnerItem> owners = mapper.refundByOwner(ctx, s, e);
        resp.setRefundByOwner(owners == null ? new ArrayList<>() : owners);

        return resp;
    }

    public List<OrderAnalyticsResp.SourceAttributionItem> sourceAttribution(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();

        List<OrderAnalyticsResp.SourceAttributionItem> items = mapper.sourceAttribution(ctx, s, e);
        if (items == null) items = new ArrayList<>();
        BigDecimal total = items.stream()
                .map(i -> i.getOrderAmount() == null ? BigDecimal.ZERO : i.getOrderAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (OrderAnalyticsResp.SourceAttributionItem it : items) {
            it.setSourceLabel(SOURCE_LABELS.getOrDefault(it.getSource(), it.getSource()));
            if (it.getOrderAmount() == null) it.setOrderAmount(BigDecimal.ZERO);
            it.setAmountRate(total.compareTo(BigDecimal.ZERO) > 0
                    ? it.getOrderAmount().multiply(BigDecimal.valueOf(100))
                        .divide(total, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
        }
        return items;
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
