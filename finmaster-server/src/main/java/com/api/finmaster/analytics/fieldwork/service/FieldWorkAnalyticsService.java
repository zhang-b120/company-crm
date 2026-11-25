package com.qykj.finmaster.analytics.fieldwork.service;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.common.context.AnalyticsContextBuilder;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.fieldwork.dto.FieldWorkAnalyticsResp;
import com.qykj.finmaster.analytics.fieldwork.mapper.FieldWorkAnalyticsMapper;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FieldWorkAnalyticsService {

    @Autowired
    private AnalyticsContextBuilder contextBuilder;

    @Autowired
    private FieldWorkAnalyticsMapper mapper;

    public FieldWorkAnalyticsResp.OverviewResp overview(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDateTime s = ctx.getTimeStart(), e = ctx.getTimeEnd();

        FieldWorkAnalyticsResp.OverviewResp resp = new FieldWorkAnalyticsResp.OverviewResp();
        resp.setScope(ctx.getScope().name());

        Long total = nvl(mapper.countTotal(ctx, s, e));
        Long completed = nvl(mapper.countByStatus(ctx, s, e, "COMPLETED"));
        Long pending = nvl(mapper.countByStatus(ctx, s, e, "PENDING"));
        Long inProgress = nvl(mapper.countByStatus(ctx, s, e, "IN_PROGRESS"));
        Long cancelled = nvl(mapper.countByStatus(ctx, s, e, "CANCELLED"));
        resp.setTotalTasks(total);
        resp.setCompletedTasks(completed);
        resp.setPendingTasks(pending);
        resp.setInProgressTasks(inProgress);
        resp.setCancelledTasks(cancelled);
        resp.setCompletionRate(total > 0
                ? BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        resp.setCancellationRate(total > 0
                ? BigDecimal.valueOf(cancelled).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        resp.setAvgActualDuration(nvlB(mapper.avgActualDuration(ctx, s, e)));
        resp.setAvgArrivalDelayMin(nvlB(mapper.avgArrivalDelay(ctx, s, e)));
        List<FieldWorkAnalyticsResp.TaskTypeItem> td = mapper.taskTypeDist(ctx, s, e);
        resp.setTaskTypeDist(td == null ? new ArrayList<>() : td);
        return resp;
    }

    public List<FieldWorkAnalyticsResp.RankItem> ranking(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        List<FieldWorkAnalyticsResp.RankItem> list = mapper.assigneeRanking(ctx, ctx.getTimeStart(), ctx.getTimeEnd());
        return list == null ? new ArrayList<>() : list;
    }

    private static Long nvl(Long v) { return v == null ? 0L : v; }
    private static BigDecimal nvlB(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
