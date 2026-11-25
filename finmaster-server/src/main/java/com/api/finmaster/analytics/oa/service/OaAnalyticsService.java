package com.qykj.finmaster.analytics.oa.service;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.common.context.AnalyticsContextBuilder;
import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.oa.dto.OaAnalyticsResp;
import com.qykj.finmaster.analytics.oa.mapper.OaAnalyticsMapper;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OaAnalyticsService {

    private static final Map<String, String> STATUS_LABELS = new HashMap<>();
    static {
        STATUS_LABELS.put("NORMAL", "正常");
        STATUS_LABELS.put("LATE", "迟到");
        STATUS_LABELS.put("EARLY", "早退");
        STATUS_LABELS.put("ABSENT", "缺勤");
        STATUS_LABELS.put("LEAVE", "请假");
        STATUS_LABELS.put("UNKNOWN", "未知");
    }

    @Autowired
    private AnalyticsContextBuilder contextBuilder;

    @Autowired
    private OaAnalyticsMapper mapper;

    public OaAnalyticsResp.AttendanceComplianceResp attendanceCompliance(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        LocalDate start = ctx.getTimeStart().toLocalDate();
        LocalDate end = ctx.getTimeEnd().toLocalDate();

        OaAnalyticsResp.AttendanceComplianceResp resp = new OaAnalyticsResp.AttendanceComplianceResp();
        resp.setScope(ctx.getScope().name());

        List<Map<String, Object>> rows = mapper.statusDistribution(ctx, start, end);
        Map<String, Long> m = new HashMap<>();
        long total = 0L;
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String st = String.valueOf(r.get("status"));
                Long c = toLong(r.get("count"));
                m.put(st, c);
                total += c;
            }
        }
        resp.setTotalRecords(total);
        resp.setNormalCount(m.getOrDefault("NORMAL", 0L));
        resp.setLateCount(m.getOrDefault("LATE", 0L));
        resp.setEarlyCount(m.getOrDefault("EARLY", 0L));
        resp.setAbsentCount(m.getOrDefault("ABSENT", 0L));
        resp.setLeaveCount(m.getOrDefault("LEAVE", 0L));

        resp.setComplianceRate(total > 0
                ? BigDecimal.valueOf(resp.getNormalCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        resp.setLateRate(total > 0
                ? BigDecimal.valueOf(resp.getLateCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        resp.setAbsentRate(total > 0
                ? BigDecimal.valueOf(resp.getAbsentCount())
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);

        List<OaAnalyticsResp.StatusItem> dist = new ArrayList<>();
        for (String key : new String[]{"NORMAL", "LATE", "EARLY", "ABSENT", "LEAVE"}) {
            OaAnalyticsResp.StatusItem it = new OaAnalyticsResp.StatusItem();
            it.setStatus(key);
            it.setStatusLabel(STATUS_LABELS.get(key));
            it.setCount(m.getOrDefault(key, 0L));
            dist.add(it);
        }
        resp.setStatusDist(dist);

        List<OaAnalyticsResp.UserCompliance> worst = mapper.worstComplianceUsers(ctx, start, end);
        resp.setWorstComplianceUsers(worst == null ? new ArrayList<>() : worst);
        return resp;
    }

    public OaAnalyticsResp.LeaveOvertimeResp leaveOvertime(LoginUser user, AnalyticsQueryReq req) {
        AnalyticsContext ctx = contextBuilder.build(user, req);
        java.time.LocalDateTime s = ctx.getTimeStart();
        java.time.LocalDateTime e = ctx.getTimeEnd();

        OaAnalyticsResp.LeaveOvertimeResp resp = new OaAnalyticsResp.LeaveOvertimeResp();
        resp.setScope(ctx.getScope().name());
        resp.setTotalLeaveDays(nvlB(mapper.sumLeaveDays(ctx, s, e)));
        resp.setApprovedLeaveCount(nvlL(mapper.countApprovedLeaves(ctx, s, e)));
        resp.setTotalOvertimeHours(nvlB(mapper.sumOvertimeHours(ctx, s, e)));
        resp.setOvertimeCount(nvlL(mapper.countOvertime(ctx, s, e)));
        List<OaAnalyticsResp.LeaveTypeItem> types = mapper.leaveTypeDist(ctx, s, e);
        resp.setLeaveTypeDist(types == null ? new ArrayList<>() : types);
        List<OaAnalyticsResp.UserLeaveItem> topUsers = mapper.topLeaveUsers(ctx, s, e);
        resp.setTopLeaveUsers(topUsers == null ? new ArrayList<>() : topUsers);
        return resp;
    }

    private static java.math.BigDecimal nvlB(java.math.BigDecimal v) { return v == null ? java.math.BigDecimal.ZERO : v; }
    private static Long nvlL(Long v) { return v == null ? 0L : v; }

    private static Long toLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception ex) { return 0L; }
    }
}
