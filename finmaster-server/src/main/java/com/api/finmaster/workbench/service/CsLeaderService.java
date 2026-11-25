package com.qykj.finmaster.workbench.service;

import com.qykj.finmaster.analytics.common.service.AnalyticsScopeResolver;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.mapper.UserDepartmentMapper;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import com.qykj.finmaster.workbench.dto.CsLeaderResp;
import com.qykj.finmaster.workbench.mapper.CsLeaderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CsLeaderService {

    @Autowired
    private CsLeaderMapper mapper;
    @Autowired
    private AnalyticsScopeResolver scopeResolver;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserDepartmentMapper userDepartmentMapper;

    private static final int TOP_LIMIT = 5;
    /** 掉队/进步榜门槛：上月完成 < 5 单的成员不入榜（避免低基数被放大成 ±30% 噪声）*/
    private static final long DELTA_PREV_MIN_COUNT = 5;
    /** 进步/掉队幅度门槛 ±30% */
    private static final int DELTA_PCT_THRESHOLD = 30;

    /**
     * 取「当前登录账号所在部门」（含子部门）下的全部成员用户名。
     * 涵盖：sys_user.dept_id 主部门 + user_department 关联多部门，递归子部门。
     * 用于诊断画像下拉等"管家可选名单"场景，**不依赖 leader_username**。
     */
    /** 团队/数据范围解析：优先按"登录账号所在部门"展开，否则兜底 leader_username 范围 */
    private List<String> resolveScopeUsernames(LoginUser user) {
        List<String> deptUsernames = resolveMyDeptUsernames(user);
        if (!deptUsernames.isEmpty()) return deptUsernames;
        AnalyticsScopeResolver.ScopeResult scope = scopeResolver.resolveTeamUsers(user);
        return (scope.getUserNameList() != null && !scope.getUserNameList().isEmpty())
                ? scope.getUserNameList()
                : Collections.singletonList(user.getUsername());
    }

    private List<String> resolveMyDeptUsernames(LoginUser user) {
        if (user == null || user.getUserId() == null) return new ArrayList<>();
        // sys_user 表无 dept_id 列，部门关系完全走 sys_user_department
        List<Long> related = userDepartmentMapper.findDepartmentIdsByUserId(user.getUserId());
        Set<Long> deptIds = new LinkedHashSet<>();
        if (related != null) deptIds.addAll(related);
        if (deptIds.isEmpty()) {
            return user.getUsername() != null
                    ? new ArrayList<>(Collections.singletonList(user.getUsername()))
                    : new ArrayList<>();
        }
        Set<String> names = new LinkedHashSet<>();
        for (Long deptId : deptIds) {
            // isChild=null 表示「本部门 + 所有子部门」；isChild=1 是只取子部门
            List<User> users = userMapper.selectUsersByDeptWithChildren(deptId, null);
            if (users != null) for (User x : users) {
                if (x != null && x.getUsername() != null) names.add(x.getUsername());
            }
        }
        if (user.getUsername() != null) names.add(user.getUsername());
        return new ArrayList<>(names);
    }

    public CsLeaderResp summary(LoginUser user) {
        CsLeaderResp resp = new CsLeaderResp();
        if (user == null || user.getTenantId() == null) return resp;
        Long tenantId = user.getTenantId();

        // 数据范围 + 诊断下拉统一按"登录账号所在部门"展开（不依赖 leader_username）
        // 否则当前账号不是 leader_username 时 resolveTeamUsers 兜底为 onlySelf，
        // 团队成员进度汇总 / A4 到期续费 / B 段对比等所有指标都会变 0。
        List<String> usernames = resolveScopeUsernames(user);
        resp.setMembers(buildMemberOptions(tenantId, usernames));

        LocalDate today = LocalDate.now();
        Date now = toDate(today);
        Date dayStart = toDate(today);
        Date weekFrom = toDate(today);
        Date weekTo = toDate(today.with(DayOfWeek.SUNDAY).plusDays(1));
        Date monthStart = toDate(today.withDayOfMonth(1));
        Date monthEnd = toDate(today.withDayOfMonth(1).plusMonths(1));
        Date prevMonthStart = toDate(today.withDayOfMonth(1).minusMonths(1));
        Date yoyMonthStart = toDate(today.withDayOfMonth(1).minusYears(1));
        Date yoyMonthEnd = toDate(today.withDayOfMonth(1).minusYears(1).plusMonths(1));
        Date sixMonthsStart = toDate(today.withDayOfMonth(1).minusMonths(5));
        String currPeriod = currentPeriod();
        List<String> sixPeriods = lastSixPeriods(currPeriod);

        resp.setProgress(buildProgress(tenantId, usernames, now, weekFrom, weekTo, currPeriod, monthStart, monthEnd));
        resp.setTrend(buildTrend(tenantId, usernames,
                monthStart, monthEnd, prevMonthStart, yoyMonthStart, yoyMonthEnd, sixMonthsStart, sixPeriods));
        return resp;
    }

    private CsLeaderResp.ProgressBlock buildProgress(Long tenantId, List<String> usernames,
                                                      Date now, Date weekFrom, Date weekTo,
                                                      String currPeriod, Date monthStart, Date monthEnd) {
        CsLeaderResp.ProgressBlock p = new CsLeaderResp.ProgressBlock();

        // A1
        List<CsLeaderResp.StackedBackLog> backlog = new ArrayList<>();
        List<Map<String, Object>> bRows = mapper.backlogByMember(tenantId, usernames);
        if (bRows != null) for (Map<String, Object> r : bRows) {
            CsLeaderResp.StackedBackLog s = new CsLeaderResp.StackedBackLog();
            s.setUsername(asString(r.get("username")));
            s.setRealName(asString(r.get("real_name")));
            s.setBookkeeping(asLong(r.get("bookkeeping")));
            s.setRenewal(asLong(r.get("renewal")));
            s.setFieldwork(asLong(r.get("fieldwork")));
            s.setDataCollection(asLong(r.get("data_collection")));
            s.setTotal(asLong(r.get("total")));
            backlog.add(s);
        }
        p.setBacklogByMember(backlog);

        // A2 超期
        List<CsLeaderResp.OverdueStat> overdues = new ArrayList<>();
        long overdueTotal = 0L;
        List<Map<String, Object>> oRows = mapper.overdueByMember(tenantId, usernames, now);
        if (oRows != null) for (Map<String, Object> r : oRows) {
            CsLeaderResp.OverdueStat s = new CsLeaderResp.OverdueStat();
            s.setUsername(asString(r.get("username")));
            s.setRealName(asString(r.get("real_name")));
            s.setOverdueCount(asLong(r.get("overdue_count")));
            s.setMaxDays(asLong(r.get("max_days")));
            overdueTotal += s.getOverdueCount();
            overdues.add(s);
        }
        p.setOverdueTotal(overdueTotal);
        p.setOverdueByMember(overdues);
        List<CsLeaderResp.OverdueDetail> top5 = new ArrayList<>();
        List<Map<String, Object>> top5Rows = mapper.overdueTop5(tenantId, usernames, now);
        if (top5Rows != null) for (Map<String, Object> r : top5Rows) {
            CsLeaderResp.OverdueDetail d = new CsLeaderResp.OverdueDetail();
            d.setWorkOrderId(asLong(r.get("work_order_id")));
            d.setTaskNo(asString(r.get("task_no")));
            d.setTitle(asString(r.get("title")));
            d.setTaskType(asString(r.get("task_type")));
            d.setOwnerName(asString(r.get("owner_name")));
            d.setOverdueDays(asLong(r.get("overdue_days")));
            top5.add(d);
        }
        p.setOverdueTop5(top5);

        // A3 stale
        List<CsLeaderResp.StaleStat> stales = new ArrayList<>();
        long staleTotal = 0L;
        List<Map<String, Object>> sRows = mapper.staleByMember(tenantId, usernames);
        if (sRows != null) for (Map<String, Object> r : sRows) {
            CsLeaderResp.StaleStat s = new CsLeaderResp.StaleStat();
            s.setUsername(asString(r.get("username")));
            s.setRealName(asString(r.get("real_name")));
            s.setCount3(asLong(r.get("count3")));
            s.setCount7(asLong(r.get("count7")));
            staleTotal += s.getCount3() + s.getCount7();
            stales.add(s);
        }
        p.setStaleTotal(staleTotal);
        p.setStaleByMember(stales);

        // A4 到期续费（基于 company_service 代账业务）：3 桶 + 2/3 月内合计
        LocalDate today2 = LocalDate.now();
        LocalDate plus1m = today2.plusMonths(1);
        LocalDate plus2m = today2.plusMonths(2);
        LocalDate plus3m = today2.plusMonths(3);
        Map<String, Object> agg = mapper.expiringRenewalAgg(usernames, today2, plus1m, plus2m, plus3m);
        if (agg != null) {
            p.setRenewBucket1m(asLong(agg.get("bucket1m")));
            p.setRenewBucket2m(asLong(agg.get("bucket2m")));
            p.setRenewBucket3m(asLong(agg.get("bucket3m")));
            p.setRenewWithin1Month(asLong(agg.get("bucket1m")));
            p.setRenewWithin2Month(asLong(agg.get("within2m")));
            p.setRenewWithin3Month(asLong(agg.get("within3m")));
        }
        List<CsLeaderResp.RenewalStat> upcomings = new ArrayList<>();
        List<Map<String, Object>> uRows = mapper.expiringRenewalByMember(tenantId, usernames, today2, plus1m, plus2m, plus3m);
        if (uRows != null) for (Map<String, Object> r : uRows) {
            CsLeaderResp.RenewalStat s = new CsLeaderResp.RenewalStat();
            s.setUsername(asString(r.get("username")));
            s.setRealName(asString(r.get("real_name")));
            s.setBucket1m(asLong(r.get("bucket1m")));
            s.setBucket2m(asLong(r.get("bucket2m")));
            s.setBucket3m(asLong(r.get("bucket3m")));
            s.setWithin2m(asLong(r.get("within2m")));
            s.setWithin3m(asLong(r.get("within3m")));
            upcomings.add(s);
        }
        p.setUpcomingByMember(upcomings);

        // A5 数据收集
        List<CsLeaderResp.DcStat> dcs = new ArrayList<>();
        long totalCo = 0L, doneCo = 0L;
        List<Map<String, Object>> dcRows = mapper.dataCollectionByMember(tenantId, usernames, currPeriod);
        if (dcRows != null) for (Map<String, Object> r : dcRows) {
            CsLeaderResp.DcStat s = new CsLeaderResp.DcStat();
            s.setUsername(asString(r.get("username")));
            s.setRealName(asString(r.get("real_name")));
            s.setTotalCompanies(asLong(r.get("total_companies")));
            s.setDoneCompanies(asLong(r.get("done_companies")));
            s.setRate(asBd(r.get("rate")));
            totalCo += s.getTotalCompanies();
            doneCo += s.getDoneCompanies();
            dcs.add(s);
        }
        p.setDataCollectionRate(totalCo > 0 ? BigDecimal.valueOf(doneCo * 100).divide(BigDecimal.valueOf(totalCo), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        p.setDataCollectionByMember(dcs);

        // A6 负载
        List<CsLeaderResp.MemberLoad> loads = new ArrayList<>();
        List<Map<String, Object>> lRows = mapper.memberLoad(tenantId, usernames);
        if (lRows != null) for (Map<String, Object> r : lRows) {
            CsLeaderResp.MemberLoad ml = new CsLeaderResp.MemberLoad();
            ml.setUsername(asString(r.get("username")));
            ml.setRealName(asString(r.get("real_name")));
            ml.setActiveCount(asLong(r.get("active_count")));
            loads.add(ml);
        }
        p.setMemberLoad(loads);

        // A7 停留时间
        List<CsLeaderResp.StageHoldTime> stages = new ArrayList<>();
        List<Map<String, Object>> stRows = mapper.stageHoldTimes(tenantId, usernames);
        if (stRows != null) for (Map<String, Object> r : stRows) {
            CsLeaderResp.StageHoldTime s = new CsLeaderResp.StageHoldTime();
            s.setStage(asString(r.get("stage")));
            s.setAvgHours(asBd(r.get("avg_hours")));
            s.setLongTailCount(asLong(r.get("long_tail_count")));
            stages.add(s);
        }
        p.setStageHoldTimes(stages);

        // A8 未续原因
        List<CsLeaderResp.PieSlice> reasons = new ArrayList<>();
        List<Map<String, Object>> nrRows = mapper.notRenewReasons(tenantId, usernames, monthStart, monthEnd);
        if (nrRows != null) for (Map<String, Object> r : nrRows) {
            CsLeaderResp.PieSlice s = new CsLeaderResp.PieSlice();
            s.setName(asString(r.get("name")));
            s.setValue(asBd(r.get("value")));
            reasons.add(s);
        }
        p.setNotRenewReasons(reasons);

        // A9 驳回
        List<CsLeaderResp.RejectStat> rejects = new ArrayList<>();
        List<Map<String, Object>> rjRows = mapper.rejectByMember(tenantId, usernames, monthStart, monthEnd);
        if (rjRows != null) for (Map<String, Object> r : rjRows) {
            CsLeaderResp.RejectStat s = new CsLeaderResp.RejectStat();
            s.setUsername(asString(r.get("username")));
            s.setRealName(asString(r.get("real_name")));
            s.setTotalSteps(asLong(r.get("total_steps")));
            s.setRejectedCount(asLong(r.get("rejected_count")));
            s.setRejectRate(asBd(r.get("reject_rate")));
            rejects.add(s);
        }
        p.setRejectByMember(rejects);

        return p;
    }

    private CsLeaderResp.TrendBlock buildTrend(Long tenantId, List<String> usernames,
                                                Date currStart, Date currEnd, Date prevStart, Date yoyStart, Date yoyEnd,
                                                Date sixMonthsStart, List<String> sixPeriods) {
        CsLeaderResp.TrendBlock t = new CsLeaderResp.TrendBlock();

        // B1 续费金额（团队 + 同环比 + 按管家）
        BigDecimal currAmt = nvlBd(mapper.teamRenewAmount(tenantId, usernames, currStart, currEnd));
        BigDecimal prevAmt = nvlBd(mapper.teamRenewAmount(tenantId, usernames, prevStart, currStart));
        BigDecimal yoyAmt = nvlBd(mapper.teamRenewAmount(tenantId, usernames, yoyStart, yoyEnd));
        t.setMonthRenewAmount(currAmt);
        t.setMonthRenewAmountGrowth(growthPct(currAmt, prevAmt));
        t.setMonthRenewAmountYoyGrowth(growthPct(currAmt, yoyAmt));
        List<CsLeaderResp.MemberRenew> renewMembers = new ArrayList<>();
        List<Map<String, Object>> rmRows = mapper.renewByMember(tenantId, usernames, currStart, currEnd, prevStart, currStart, yoyStart, yoyEnd);
        if (rmRows != null) for (Map<String, Object> r : rmRows) {
            CsLeaderResp.MemberRenew m = new CsLeaderResp.MemberRenew();
            m.setUsername(asString(r.get("username")));
            m.setRealName(asString(r.get("real_name")));
            BigDecimal cur = asBd(r.get("current"));
            BigDecimal prev = asBd(r.get("previous"));
            BigDecimal yoy = asBd(r.get("yoy"));
            m.setCurrent(cur);
            m.setPrevious(prev);
            m.setYoy(yoy);
            m.setGrowth(growthPct(cur, prev));
            m.setYoyGrowth(growthPct(cur, yoy));
            renewMembers.add(m);
        }
        t.setRenewByMember(renewMembers);

        // B2 续费率三列
        Map<String, Object> rrCurr = mapper.teamRenewRate(tenantId, usernames, currStart, currEnd);
        Map<String, Object> rrPrev = mapper.teamRenewRate(tenantId, usernames, prevStart, currStart);
        Map<String, Object> rrYoy = mapper.teamRenewRate(tenantId, usernames, yoyStart, yoyEnd);
        t.setMonthRate(rrCurr == null ? BigDecimal.ZERO : asBd(rrCurr.get("rate")));
        t.setPrevMonthRate(rrPrev == null ? BigDecimal.ZERO : asBd(rrPrev.get("rate")));
        t.setYoyRate(rrYoy == null ? BigDecimal.ZERO : asBd(rrYoy.get("rate")));
        List<CsLeaderResp.MemberRate> rateMembers = new ArrayList<>();
        List<Map<String, Object>> rrRows = mapper.renewRateByMember(tenantId, usernames, currStart, currEnd, prevStart, currStart);
        if (rrRows != null) for (Map<String, Object> r : rrRows) {
            CsLeaderResp.MemberRate m = new CsLeaderResp.MemberRate();
            m.setUsername(asString(r.get("username")));
            m.setRealName(asString(r.get("real_name")));
            m.setCurrent(asBd(r.get("current")));
            m.setPrevious(asBd(r.get("previous")));
            rateMembers.add(m);
        }
        t.setRateByMember(rateMembers);

        // B3 6 月续费金额走势
        List<CsLeaderResp.MonthlySeries> series = new ArrayList<>();
        CsLeaderResp.MonthlySeries teamS = new CsLeaderResp.MonthlySeries();
        teamS.setName("团队总"); teamS.setType("TEAM");
        teamS.setPoints(toMonthValues(mapper.teamRenewMonthly(tenantId, usernames, sixMonthsStart, currEnd)));
        series.add(teamS);
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        Map<String, String> nameMap = new HashMap<>();
        List<Map<String, Object>> mmRows = mapper.memberRenewMonthly(tenantId, usernames, sixMonthsStart, currEnd);
        if (mmRows != null) for (Map<String, Object> r : mmRows) {
            String un = asString(r.get("username"));
            grouped.computeIfAbsent(un, k -> new ArrayList<>()).add(r);
            if (!nameMap.containsKey(un)) nameMap.put(un, asString(r.get("real_name")));
        }
        List<MemberDelta> deltas = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : grouped.entrySet()) {
            List<Map<String, Object>> rows = e.getValue();
            if (rows.size() < 2) continue;
            BigDecimal first = asBd(rows.get(0).get("amount"));
            BigDecimal last = asBd(rows.get(rows.size() - 1).get("amount"));
            BigDecimal delta = first.signum() == 0 ? (last.signum() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                    : last.subtract(first).multiply(BigDecimal.valueOf(100)).divide(first, 2, RoundingMode.HALF_UP);
            deltas.add(new MemberDelta(e.getKey(), nameMap.get(e.getKey()), delta));
        }
        deltas.sort((a, b) -> b.delta.compareTo(a.delta));
        List<MemberDelta> ups = deltas.stream().filter(d -> d.delta.signum() > 0).limit(TOP_LIMIT).collect(Collectors.toList());
        List<MemberDelta> downs = deltas.stream().filter(d -> d.delta.signum() < 0)
                .sorted(Comparator.comparing(d -> d.delta)).limit(TOP_LIMIT).collect(Collectors.toList());
        for (MemberDelta md : ups) {
            CsLeaderResp.MonthlySeries s = new CsLeaderResp.MonthlySeries();
            s.setName(md.realName != null ? md.realName : md.username);
            s.setType("UP");
            s.setPoints(toMonthValues(grouped.get(md.username)));
            series.add(s);
        }
        for (MemberDelta md : downs) {
            CsLeaderResp.MonthlySeries s = new CsLeaderResp.MonthlySeries();
            s.setName(md.realName != null ? md.realName : md.username);
            s.setType("DOWN");
            s.setPoints(toMonthValues(grouped.get(md.username)));
            series.add(s);
        }
        t.setRenewMonthlySeries(series);

        // B4 完成率排行
        List<CsLeaderResp.CompletionRank> ranks = new ArrayList<>();
        List<Map<String, Object>> cRows = mapper.completionRanking(tenantId, usernames, currStart, currEnd);
        int idx = 0;
        if (cRows != null) for (Map<String, Object> r : cRows) {
            CsLeaderResp.CompletionRank cr = new CsLeaderResp.CompletionRank();
            cr.setRank(++idx);
            cr.setUsername(asString(r.get("username")));
            cr.setRealName(asString(r.get("real_name")));
            cr.setDoneCount(asLong(r.get("done_count")));
            cr.setTotalCount(asLong(r.get("total_count")));
            cr.setRate(asBd(r.get("rate")));
            ranks.add(cr);
        }
        t.setCompletionRanking(ranks);

        // B5 6 月平均时长
        List<CsLeaderResp.MonthlySeries> hourSeries = new ArrayList<>();
        CsLeaderResp.MonthlySeries hSeries = new CsLeaderResp.MonthlySeries();
        hSeries.setName("团队总"); hSeries.setType("TEAM");
        hSeries.setPoints(toMonthValues(mapper.teamAvgHourMonthly(tenantId, usernames, sixMonthsStart, currEnd)));
        hourSeries.add(hSeries);
        t.setAvgHourSeries(hourSeries);

        // B6 数据收集 6 账期
        List<CsLeaderResp.MonthValue> dcSeries = new ArrayList<>();
        List<Map<String, Object>> dcRows = mapper.dcRateSeries(tenantId, usernames, sixPeriods);
        if (dcRows != null) for (Map<String, Object> r : dcRows) {
            CsLeaderResp.MonthValue v = new CsLeaderResp.MonthValue();
            v.setMonth(asString(r.get("month")));
            v.setValue(asBd(r.get("amount")));
            dcSeries.add(v);
        }
        t.setDcRateSeries(dcSeries);

        // B7 掉队/进步
        List<Map<String, Object>> compareRows = mapper.doneCountCompare(tenantId, usernames, currStart, currEnd, prevStart, currStart);
        List<CsLeaderResp.MemberDelta> drops = new ArrayList<>();
        List<CsLeaderResp.MemberDelta> upMembers = new ArrayList<>();
        if (compareRows != null) for (Map<String, Object> r : compareRows) {
            long currN = asLong(r.get("curr_count"));
            long prevN = asLong(r.get("prev_count"));
            // 低基数过滤：上月 < 5 单 / 本月 0 单 一律不入榜
            if (prevN < DELTA_PREV_MIN_COUNT || currN <= 0) continue;
            BigDecimal curr = BigDecimal.valueOf(currN);
            BigDecimal prev = BigDecimal.valueOf(prevN);
            BigDecimal pct = curr.subtract(prev).multiply(BigDecimal.valueOf(100))
                    .divide(prev, 2, RoundingMode.HALF_UP);
            CsLeaderResp.MemberDelta md = new CsLeaderResp.MemberDelta();
            md.setUsername(asString(r.get("username")));
            md.setRealName(asString(r.get("real_name")));
            md.setCurrentValue(curr);
            md.setPreviousValue(prev);
            md.setChangePct(pct);
            if (pct.compareTo(BigDecimal.valueOf(-DELTA_PCT_THRESHOLD)) <= 0) drops.add(md);
            else if (pct.compareTo(BigDecimal.valueOf(DELTA_PCT_THRESHOLD)) >= 0) upMembers.add(md);
        }
        drops.sort(Comparator.comparing(CsLeaderResp.MemberDelta::getChangePct));
        upMembers.sort((a, b) -> b.getChangePct().compareTo(a.getChangePct()));
        t.setDropdownMembers(drops);
        t.setUprisingMembers(upMembers);

        return t;
    }

    public CsLeaderResp.ButlerDiagnosis diagnose(LoginUser user, String username) {
        if (username == null) return null;
        Long tenantId = user.getTenantId();
        CsLeaderResp.ButlerDiagnosis d = new CsLeaderResp.ButlerDiagnosis();
        d.setUsername(username);

        LocalDate today = LocalDate.now();
        Date sixStart = toDate(today.withDayOfMonth(1).minusMonths(5));
        Date now = toDate(today);
        Date weekFrom = toDate(today);
        Date weekTo = toDate(today.with(DayOfWeek.SUNDAY).plusDays(1));
        Date monthStart = toDate(today.withDayOfMonth(1));
        Date monthEnd = toDate(today.withDayOfMonth(1).plusMonths(1));

        d.setLoadSeries6m(toMonthValues(mapper.butlerLoadSeries(tenantId, username, sixStart, now)));
        d.setAvgHourSeries6m(toMonthValues(mapper.butlerAvgHourSeries(tenantId, username, sixStart, now)));
        d.setRenewRateSeries6m(toMonthValues(mapper.butlerRenewRateSeries(tenantId, username, sixStart, now)));
        Map<String, Object> rj = mapper.butlerRejectRate(tenantId, username, monthStart, monthEnd);
        d.setRejectRate(rj == null ? BigDecimal.ZERO : asBd(rj.get("rate")));
        d.setUpcomingRenewalCount(nvl(mapper.butlerUpcomingRenewal(tenantId, username, weekFrom, weekTo)));
        return d;
    }

    private List<CsLeaderResp.MemberOption> buildMemberOptions(Long tenantId, List<String> usernames) {
        List<CsLeaderResp.MemberOption> opts = new ArrayList<>();
        if (usernames == null || usernames.isEmpty()) return opts;
        // 走 sys_user 拉部门下全部成员（含目前没有 work_order 的人）
        List<Map<String, Object>> rows = mapper.listMembersByUsernames(tenantId, usernames);
        if (rows != null) for (Map<String, Object> r : rows) {
            String un = asString(r.get("username"));
            if (un == null) continue;
            CsLeaderResp.MemberOption o = new CsLeaderResp.MemberOption();
            o.setUsername(un);
            o.setRealName(asString(r.get("real_name")));
            opts.add(o);
        }
        return opts;
    }

    // ===== 工具 =====
    private static class MemberDelta {
        String username, realName;
        BigDecimal delta;
        MemberDelta(String u, String r, BigDecimal d) { username = u; realName = r; delta = d; }
    }
    private List<CsLeaderResp.MonthValue> toMonthValues(List<Map<String, Object>> rows) {
        List<CsLeaderResp.MonthValue> list = new ArrayList<>();
        if (rows == null) return list;
        for (Map<String, Object> r : rows) {
            CsLeaderResp.MonthValue v = new CsLeaderResp.MonthValue();
            v.setMonth(asString(r.get("month")));
            v.setValue(asBd(r.get("amount")));
            list.add(v);
        }
        return list;
    }
    private static String currentPeriod() {
        LocalDate today = LocalDate.now();
        LocalDate p = today.getDayOfMonth() < 15 ? today.minusMonths(1).withDayOfMonth(1) : today.withDayOfMonth(1);
        return p.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
    private static List<String> lastSixPeriods(String curr) {
        LocalDate c = LocalDate.parse(curr);
        List<String> list = new ArrayList<>();
        for (int i = 5; i >= 0; i--) list.add(c.minusMonths(i).format(DateTimeFormatter.ISO_LOCAL_DATE));
        return list;
    }
    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
    private static String asString(Object o) { return o == null ? null : o.toString(); }
    private static long asLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }
    private static BigDecimal asBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(o.toString());
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
    private static long nvl(Long v) { return v == null ? 0L : v; }
    private static BigDecimal nvlBd(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static BigDecimal growthPct(BigDecimal cur, BigDecimal prev) {
        if (cur == null) cur = BigDecimal.ZERO;
        if (prev == null || prev.signum() <= 0) return cur.signum() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        return cur.subtract(prev).multiply(BigDecimal.valueOf(100)).divide(prev, 2, RoundingMode.HALF_UP);
    }
}
