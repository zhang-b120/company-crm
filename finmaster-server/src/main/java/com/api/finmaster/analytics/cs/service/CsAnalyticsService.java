package com.qykj.finmaster.analytics.cs.service;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.common.context.AnalyticsContextBuilder;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.analytics.common.util.TrendBuilder;
import com.qykj.finmaster.analytics.cs.dto.CsAnalyticsResp;
import com.qykj.finmaster.analytics.cs.mapper.CsAnalyticsMapper;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CsAnalyticsService {

    private static final Map<Integer, String> RENEW_TYPE_LABELS = new HashMap<>();
    static {
        RENEW_TYPE_LABELS.put(0, "正常续费");
        RENEW_TYPE_LABELS.put(1, "提前续费");
        RENEW_TYPE_LABELS.put(2, "延期续费");
        RENEW_TYPE_LABELS.put(3, "未续费");
    }

    @Autowired
    private AnalyticsContextBuilder contextBuilder;

    @Autowired
    private CsAnalyticsMapper mapper;

    public CsAnalyticsResp.RenewalResp renewal(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDate sd = ctx.getTimeStart().toLocalDate();
        LocalDate ed = ctx.getTimeEnd().toLocalDate();

        CsAnalyticsResp.RenewalResp resp = new CsAnalyticsResp.RenewalResp();
        resp.setScope(ctx.getScope().name());

        Long expiring = nvlLong(mapper.countExpiring(ctx, sd, ed));
        Long renewed = nvlLong(mapper.countRenewed(ctx, sd, ed));
        Long notRenewed = nvlLong(mapper.countNotRenewed(ctx, sd, ed));
        BigDecimal amount = nvl(mapper.sumRenewAmount(ctx, sd, ed));

        resp.setExpiringCount(expiring);
        resp.setRenewedCount(renewed);
        resp.setNotRenewedCount(notRenewed);
        resp.setRenewAmount(amount);
        resp.setRenewalRate(expiring > 0
                ? BigDecimal.valueOf(renewed).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(expiring), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        // 续费类型分布
        List<CsAnalyticsResp.RenewTypeItem> dist = mapper.renewTypeDist(ctx, sd, ed);
        if (dist == null) dist = new ArrayList<>();
        for (CsAnalyticsResp.RenewTypeItem it : dist) {
            it.setTypeLabel(RENEW_TYPE_LABELS.getOrDefault(it.getType(), "其他"));
            if (it.getAmount() == null) it.setAmount(BigDecimal.ZERO);
        }
        resp.setRenewTypeDist(dist);

        // 管家排行
        List<CsAnalyticsResp.ButlerRankItem> br = mapper.butlerRanking(ctx, sd, ed);
        resp.setButlerRanking(br == null ? new ArrayList<>() : br);

        // 趋势
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();
        TrendBuilder.Granularity g = TrendBuilder.pickGranularity(s, e);
        List<Map<String, Object>> rows = g == TrendBuilder.Granularity.DAY
                ? mapper.renewTrendByDay(ctx, s, e)
                : mapper.renewTrendByMonth(ctx, s, e);
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
        resp.setRenewTrend(trend);

        return resp;
    }

    public CsAnalyticsResp.TaskSummaryResp taskSummary(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();

        CsAnalyticsResp.TaskSummaryResp resp = new CsAnalyticsResp.TaskSummaryResp();
        resp.setScope(ctx.getScope().name());

        Long total = nvlLong(mapper.countTotalTasks(ctx, s, e));
        Long completed = nvlLong(mapper.countTasks(ctx, s, e, "COMPLETED"));
        Long finished = nvlLong(mapper.countTasks(ctx, s, e, "FINISHED"));
        Long cancelled = nvlLong(mapper.countTasks(ctx, s, e, "CANCELLED"));
        Long overdue = nvlLong(mapper.countOverdueTasks(ctx, s, e));
        long done = completed + finished;

        resp.setTotalTasks(total);
        resp.setCompletedTasks(done);
        resp.setInProgressTasks(Math.max(0L, total - done - cancelled));
        resp.setCancelledTasks(cancelled);
        resp.setOverdueTasks(overdue);
        resp.setCompletionRate(total > 0
                ? BigDecimal.valueOf(done).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        resp.setAvgProcessingDays(nvl(mapper.avgProcessingDays(ctx, s, e)));

        // 状态分布
        List<Map<String, Object>> rows = mapper.taskStatusDist(ctx, s, e);
        List<CsAnalyticsResp.StatusItem> items = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                CsAnalyticsResp.StatusItem it = new CsAnalyticsResp.StatusItem();
                it.setStatus(String.valueOf(r.get("status")));
                it.setCount(toLong(r.get("count")));
                items.add(it);
            }
        }
        resp.setStatusDist(items);

        // 处理人排行
        List<CsAnalyticsResp.TaskOwnerRank> rk = mapper.taskOwnerRanking(ctx, s, e);
        resp.setOwnerRanking(rk == null ? new ArrayList<>() : rk);

        return resp;
    }

    public CsAnalyticsResp.NotRenewReasonsResp notRenewReasons(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDate sd = ctx.getTimeStart().toLocalDate();
        LocalDate ed = ctx.getTimeEnd().toLocalDate();

        List<CsAnalyticsResp.StringKeyCount> reasons = mapper.notRenewReasons(ctx, sd, ed);
        Long unknown = nvlLong(mapper.countNotRenewedWithoutReason(ctx, sd, ed));
        Long notRenewed = nvlLong(mapper.countNotRenewed(ctx, sd, ed));

        CsAnalyticsResp.NotRenewReasonsResp resp = new CsAnalyticsResp.NotRenewReasonsResp();
        resp.setReasons(reasons == null ? new ArrayList<>() : reasons);
        resp.setTotalNotRenewed(notRenewed);
        resp.setUnknownReasonRate(notRenewed > 0
                ? BigDecimal.valueOf(unknown).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(notRenewed), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        return resp;
    }

    public CsAnalyticsResp.CustomerProfileResp customerProfile(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);

        CsAnalyticsResp.CustomerProfileResp resp = new CsAnalyticsResp.CustomerProfileResp();
        List<CsAnalyticsResp.StarLevelItem> stars = mapper.starLevelDist(ctx);
        // 补全缺失的星级（0-5）
        Map<Integer, Long> m = new HashMap<>();
        if (stars != null) {
            for (CsAnalyticsResp.StarLevelItem it : stars) {
                Integer lvl = it.getStarLevel() == null ? 0 : it.getStarLevel();
                m.put(lvl, it.getCount() == null ? 0L : it.getCount());
            }
        }
        List<CsAnalyticsResp.StarLevelItem> filled = new ArrayList<>();
        for (int i : Arrays.asList(0, 1, 2, 3, 4, 5)) {
            CsAnalyticsResp.StarLevelItem it = new CsAnalyticsResp.StarLevelItem();
            it.setStarLevel(i);
            it.setCount(m.getOrDefault(i, 0L));
            filled.add(it);
        }
        resp.setStarLevelDist(filled);

        resp.setBookkeepingDist(nullToEmpty(mapper.bookkeepingDist(ctx)));
        resp.setFilingDist(nullToEmpty(mapper.filingDist(ctx)));
        return resp;
    }

    private static BigDecimal nvl(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static Long nvlLong(Long v) { return v == null ? 0L : v; }
    private static <T> List<T> nullToEmpty(List<T> l) { return l == null ? new ArrayList<>() : l; }
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
