package com.qykj.finmaster.workbench.service;

import com.qykj.finmaster.analytics.common.service.AnalyticsScopeResolver;
import com.qykj.finmaster.customer.lead.constants.LeadRecycleConstants;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import com.qykj.finmaster.workbench.dto.SalesWorkbenchResp;
import com.qykj.finmaster.workbench.mapper.SalesWorkbenchMapper;
import com.qykj.finmaster.workbench.util.PeriodCompare;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 销售工作台
 * - scope=PERSONAL：仅当前登录用户
 * - scope=TEAM：登录用户作为 leader 的部门下所有用户（复用 AnalyticsScopeResolver）
 * - 每段时间窗独立 (TODAY / WEEK / MONTH / CUSTOM)
 * - 每个核心指标返回 growth + yoyGrowth（环比 / 同比，%）
 * - byMember 明细查询用于前端弹窗
 */
@Service
public class SalesWorkbenchService {

    @Autowired
    private SalesWorkbenchMapper mapper;

    @Autowired
    private SysBizConfigService bizConfigService;

    @Autowired
    private CommissionTierService commissionTierService;

    @Autowired
    private AnalyticsScopeResolver scopeResolver;

    @Autowired
    private UserMapper userMapper;

    private static final int WARN_DAYS = 3;

    /** 销售部（含子部门）成员 username 列表：用于"即将回收"等与 LeadAutoInvalidateTask 对齐的过滤 */
    private List<String> resolveSalesDeptUsernames() {
        List<User> users = userMapper.selectUsersByDeptWithChildren(LeadRecycleConstants.SALES_DEPT_ID, null);
        if (users == null || users.isEmpty()) return Collections.emptyList();
        List<String> names = new ArrayList<>();
        for (User u : users) {
            if (u != null && u.getUsername() != null && !u.getUsername().isEmpty()) {
                names.add(u.getUsername());
            }
        }
        return names;
    }

    public enum PeriodType { TODAY, WEEK, MONTH, CUSTOM }
    public enum Scope { PERSONAL, TEAM }

    @lombok.Data
    public static class SummaryReq {
        private Scope scope = Scope.PERSONAL;
        private PeriodType signPeriod = PeriodType.TODAY;
        private LocalDate signStart;
        private LocalDate signEnd;
        private PeriodType leadPeriod = PeriodType.MONTH;
        private LocalDate leadStart;
        private LocalDate leadEnd;
        private PeriodType customerPeriod = PeriodType.MONTH;
        private LocalDate customerStart;
        private LocalDate customerEnd;
    }

    public SalesWorkbenchResp summary(LoginUser user, SummaryReq req) {
        SalesWorkbenchResp resp = new SalesWorkbenchResp();
        if (user == null || user.getTenantId() == null || user.getUsername() == null) {
            return resp;
        }
        if (req == null) req = new SummaryReq();
        resp.setScope(req.scope.name());

        Long tenantId = user.getTenantId();
        List<String> usernames = resolveUsernames(user, req.scope);

        Range signRange = resolveRange(req.signPeriod, req.signStart, req.signEnd);
        Range leadRange = resolveRange(req.leadPeriod, req.leadStart, req.leadEnd);
        Range custRange = resolveRange(req.customerPeriod, req.customerStart, req.customerEnd);

        resp.setSignRanking(buildSignRanking(tenantId, user.getUsername(), usernames, req.signPeriod, signRange, req.scope));
        resp.setLeadStats(buildLeadStats(tenantId, usernames, req.leadPeriod, leadRange));
        resp.setCustomerStats(buildCustomerStats(tenantId, usernames, req.customerPeriod, custRange));
        resp.setPerformance(buildPerformance(tenantId, user.getUsername(), usernames, req.scope));
        return resp;
    }

    /** 解析 scope 对应的 username 集合 */
    private List<String> resolveUsernames(LoginUser user, Scope scope) {
        if (scope == Scope.TEAM) {
            AnalyticsScopeResolver.ScopeResult res = scopeResolver.resolveTeamUsers(user);
            List<String> names = res.getUserNameList();
            if (names == null || names.isEmpty()) {
                // 不是任何部门 leader → 退化为个人
                return Collections.singletonList(user.getUsername());
            }
            return names;
        }
        return Collections.singletonList(user.getUsername());
    }

    // ─── 时间窗 ──────────────────────────────────────────────────────────
    private static class Range {
        LocalDateTime start;
        LocalDateTime end;
        String label;
        Range(LocalDateTime s, LocalDateTime e, String l) { start = s; end = e; label = l; }
    }

    private Range resolveRange(PeriodType type, LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        if (type == null) type = PeriodType.TODAY;
        switch (type) {
            case TODAY:
                return new Range(today.atStartOfDay(), today.plusDays(1).atStartOfDay(), today.toString());
            case WEEK: {
                LocalDate ws = today.with(DayOfWeek.MONDAY);
                return new Range(ws.atStartOfDay(), ws.plusWeeks(1).atStartOfDay(),
                        ws + " ~ " + ws.plusDays(6));
            }
            case MONTH: {
                LocalDate ms = today.withDayOfMonth(1);
                return new Range(ms.atStartOfDay(), ms.plusMonths(1).atStartOfDay(),
                        ms + " ~ " + ms.plusMonths(1).minusDays(1));
            }
            default: {
                LocalDate s = start != null ? start : today;
                LocalDate e = end != null ? end : today;
                return new Range(s.atStartOfDay(), e.plusDays(1).atStartOfDay(), s + " ~ " + e);
            }
        }
    }

    // ─── 签单排名 ─────────────────────────────────────────────────────
    private SalesWorkbenchResp.SignRankingBlock buildSignRanking(
            Long tenantId, String myName, List<String> usernames, PeriodType type, Range range, Scope scope) {
        SalesWorkbenchResp.SignRankingBlock b = new SalesWorkbenchResp.SignRankingBlock();
        b.setPeriodType(type.name());
        b.setPeriodLabel(range.label);

        // 当前值
        long myCount;
        if (scope == Scope.TEAM) {
            myCount = nvl(mapper.countTeamSignOrders(tenantId, usernames, range.start, range.end));
        } else {
            myCount = nvl(mapper.countMySignOrders(tenantId, myName, range.start, range.end));
        }
        b.setMyCount(myCount);

        // 排名（个人视角才有意义；团队视角时 myRank 用排行第一名当做"团队总数"，前端可隐藏排名）
        List<Map<String, Object>> rows = mapper.rankSignCount(tenantId, range.start, range.end);
        if (rows == null) rows = new ArrayList<>();
        if (scope == Scope.PERSONAL) {
            int totalRanked = rows.size();
            Integer myIdx = null;
            for (int i = 0; i < rows.size(); i++) {
                if (myName.equals(asString(rows.get(i).get("username")))) { myIdx = i; break; }
            }
            b.setTotalRanked(totalRanked);
            Integer rank;
            if (myIdx != null) rank = myIdx + 1;
            else if (myCount > 0) rank = totalRanked + 1;
            else rank = null;
            b.setMyRank(rank);
            if (!rows.isEmpty()) {
                long topCount = asLong(rows.get(0).get("count"));
                b.setTopCount(topCount);
                b.setTopName(realNameOrUsername(rows.get(0)));
                b.setDiffToTop(Math.max(topCount - myCount, 0));
            } else {
                b.setTopCount(0L);
                b.setDiffToTop(0L);
            }
            if (myIdx != null) {
                if (myIdx > 0) {
                    b.setPrevName(realNameOrUsername(rows.get(myIdx - 1)));
                    b.setDiffToPrev(Math.max(asLong(rows.get(myIdx - 1).get("count")) - myCount, 0));
                } else b.setDiffToPrev(0L);
                if (myIdx < rows.size() - 1) {
                    b.setNextName(realNameOrUsername(rows.get(myIdx + 1)));
                    b.setLeadOverNext(Math.max(myCount - asLong(rows.get(myIdx + 1).get("count")), 0));
                }
            }
        }

        // 同环比
        PeriodCompare.Range prev = PeriodCompare.prevByType(type.name(), range.start, range.end);
        PeriodCompare.Range yoy = PeriodCompare.yoy(range.start, range.end);
        long prevCount = scope == Scope.TEAM
                ? nvl(mapper.countTeamSignOrders(tenantId, usernames, prev.start, prev.end))
                : nvl(mapper.countMySignOrders(tenantId, myName, prev.start, prev.end));
        long yoyCount = scope == Scope.TEAM
                ? nvl(mapper.countTeamSignOrders(tenantId, usernames, yoy.start, yoy.end))
                : nvl(mapper.countMySignOrders(tenantId, myName, yoy.start, yoy.end));
        b.setMyCountGrowth(growthPct(myCount, prevCount));
        b.setMyCountYoyGrowth(growthPct(myCount, yoyCount));
        return b;
    }

    // ─── 我的线索 ─────────────────────────────────────────────────────
    private SalesWorkbenchResp.LeadStatsBlock buildLeadStats(
            Long tenantId, List<String> usernames, PeriodType type, Range range) {
        SalesWorkbenchResp.LeadStatsBlock b = new SalesWorkbenchResp.LeadStatsBlock();
        b.setPeriodType(type.name());
        b.setPeriodLabel(range.label);

        long newCount = nvl(mapper.countTeamLeadCreatedInRange(tenantId, usernames, range.start, range.end));
        long converted = nvl(mapper.countTeamLeadConvertedInRange(tenantId, usernames, range.start, range.end));
        long followed = nvl(mapper.countTeamLeadFollowedInRange(tenantId, usernames, range.start, range.end));

        int noFollowDays = bizConfigService.getGroupInt("lead.recycle", "no_follow_days", 14);
        int from = Math.max(noFollowDays - WARN_DAYS, 0);
        // 销售部成员：与 LeadAutoInvalidateTask 对齐的 ⑥ 指定人员过滤
        long aboutRecycle = nvl(mapper.countTeamLeadAboutRecycle(tenantId, usernames, from, noFollowDays, resolveSalesDeptUsernames()));

        b.setNewCount(newCount);
        b.setFollowedCount(followed);
        b.setAboutToRecycle(aboutRecycle);
        b.setConversionRate(percent(converted, newCount));

        // 同环比
        PeriodCompare.Range prev = PeriodCompare.prevByType(type.name(), range.start, range.end);
        PeriodCompare.Range yoy = PeriodCompare.yoy(range.start, range.end);
        long prevNew = nvl(mapper.countTeamLeadCreatedInRange(tenantId, usernames, prev.start, prev.end));
        long yoyNew = nvl(mapper.countTeamLeadCreatedInRange(tenantId, usernames, yoy.start, yoy.end));
        long prevFollowed = nvl(mapper.countTeamLeadFollowedInRange(tenantId, usernames, prev.start, prev.end));
        long yoyFollowed = nvl(mapper.countTeamLeadFollowedInRange(tenantId, usernames, yoy.start, yoy.end));
        long prevConverted = nvl(mapper.countTeamLeadConvertedInRange(tenantId, usernames, prev.start, prev.end));
        long yoyConverted = nvl(mapper.countTeamLeadConvertedInRange(tenantId, usernames, yoy.start, yoy.end));
        b.setNewCountGrowth(growthPct(newCount, prevNew));
        b.setNewCountYoyGrowth(growthPct(newCount, yoyNew));
        b.setFollowedCountGrowth(growthPct(followed, prevFollowed));
        b.setFollowedCountYoyGrowth(growthPct(followed, yoyFollowed));
        b.setConversionRateGrowth(growthPct(percent(converted, newCount).doubleValue(),
                percent(prevConverted, prevNew).doubleValue()));
        b.setConversionRateYoyGrowth(growthPct(percent(converted, newCount).doubleValue(),
                percent(yoyConverted, yoyNew).doubleValue()));
        return b;
    }

    // ─── 我的客户 ─────────────────────────────────────────────────────
    private SalesWorkbenchResp.CustomerStatsBlock buildCustomerStats(
            Long tenantId, List<String> usernames, PeriodType type, Range range) {
        SalesWorkbenchResp.CustomerStatsBlock b = new SalesWorkbenchResp.CustomerStatsBlock();
        b.setPeriodType(type.name());
        b.setPeriodLabel(range.label);

        long newCount = nvl(mapper.countTeamCustomerCreatedInRange(tenantId, usernames, range.start, range.end));
        long deal = nvl(mapper.countTeamCustomerDealInRange(tenantId, usernames, range.start, range.end));
        long followed = nvl(mapper.countTeamCustomerFollowedInRange(tenantId, usernames, range.start, range.end));

        int noFollowDays = bizConfigService.getGroupInt("customer.recycle", "no_follow_days", 30);
        int from = Math.max(noFollowDays - WARN_DAYS, 0);
        long aboutRecycle = nvl(mapper.countTeamCustomerAboutRecycle(tenantId, usernames, from, noFollowDays));

        b.setNewCount(newCount);
        b.setFollowedCount(followed);
        b.setAboutToRecycle(aboutRecycle);
        b.setDealRate(percent(deal, newCount));

        PeriodCompare.Range prev = PeriodCompare.prevByType(type.name(), range.start, range.end);
        PeriodCompare.Range yoy = PeriodCompare.yoy(range.start, range.end);
        long prevNew = nvl(mapper.countTeamCustomerCreatedInRange(tenantId, usernames, prev.start, prev.end));
        long yoyNew = nvl(mapper.countTeamCustomerCreatedInRange(tenantId, usernames, yoy.start, yoy.end));
        long prevFollowed = nvl(mapper.countTeamCustomerFollowedInRange(tenantId, usernames, prev.start, prev.end));
        long yoyFollowed = nvl(mapper.countTeamCustomerFollowedInRange(tenantId, usernames, yoy.start, yoy.end));
        long prevDeal = nvl(mapper.countTeamCustomerDealInRange(tenantId, usernames, prev.start, prev.end));
        long yoyDeal = nvl(mapper.countTeamCustomerDealInRange(tenantId, usernames, yoy.start, yoy.end));
        b.setNewCountGrowth(growthPct(newCount, prevNew));
        b.setNewCountYoyGrowth(growthPct(newCount, yoyNew));
        b.setFollowedCountGrowth(growthPct(followed, prevFollowed));
        b.setFollowedCountYoyGrowth(growthPct(followed, yoyFollowed));
        b.setDealRateGrowth(growthPct(percent(deal, newCount).doubleValue(),
                percent(prevDeal, prevNew).doubleValue()));
        b.setDealRateYoyGrowth(growthPct(percent(deal, newCount).doubleValue(),
                percent(yoyDeal, yoyNew).doubleValue()));
        return b;
    }

    // ─── 本月业绩 + 提成 ──────────────────────────────────────────────
    private SalesWorkbenchResp.PerformanceBlock buildPerformance(
            Long tenantId, String myName, List<String> usernames, Scope scope) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime mStart = monthStart.atStartOfDay();
        LocalDateTime mEnd = monthStart.plusMonths(1).atStartOfDay();

        BigDecimal monthAmount = scope == Scope.TEAM
                ? mapper.sumTeamSignAmount(tenantId, usernames, mStart, mEnd)
                : mapper.sumMySignAmount(tenantId, myName, mStart, mEnd);
        if (monthAmount == null) monthAmount = BigDecimal.ZERO;

        Map<String, Object> bizReg = scope == Scope.TEAM
                ? mapper.sumTeamBusinessRegProfit(tenantId, usernames, mStart, mEnd)
                : mapper.sumMyBusinessRegProfit(tenantId, myName, mStart, mEnd);
        BigDecimal bizRevenue = bizReg == null ? BigDecimal.ZERO : asBd(bizReg.get("revenue"));
        BigDecimal bizCost = bizReg == null ? BigDecimal.ZERO : asBd(bizReg.get("cost"));
        BigDecimal bizProfit = bizRevenue.subtract(bizCost);
        if (bizProfit.signum() < 0) bizProfit = BigDecimal.ZERO;

        BigDecimal performanceAmount = monthAmount.subtract(bizRevenue);
        if (performanceAmount.signum() < 0) performanceAmount = BigDecimal.ZERO;

        SalesWorkbenchResp.CommissionTierBlock legacy =
                commissionTierService.computeCommission(tenantId, monthAmount, performanceAmount, bizProfit);

        SalesWorkbenchResp.PerformanceBlock p = new SalesWorkbenchResp.PerformanceBlock();
        p.setCurrentMonthAmount(legacy.getCurrentMonthAmount());
        p.setCurrentTierName(legacy.getCurrentTierName());
        p.setCurrentRate(legacy.getCurrentRate());
        p.setNextTierThreshold(legacy.getNextTierThreshold());
        p.setAmountToNextTier(legacy.getAmountToNextTier());
        p.setNextTierRate(legacy.getNextTierRate());
        p.setBusinessRegProfit(legacy.getBusinessRegProfit());
        p.setBusinessRegCommission(legacy.getBusinessRegCommission());
        p.setPerformanceCommission(legacy.getPerformanceCommission());
        p.setTotalCommission(legacy.getTotalCommission());

        // 业绩同环比（按整月）
        LocalDate prevMonthStart = monthStart.minusMonths(1);
        LocalDate yoyMonthStart = monthStart.minusYears(1);
        BigDecimal prevAmount = scope == Scope.TEAM
                ? mapper.sumTeamSignAmount(tenantId, usernames, prevMonthStart.atStartOfDay(), monthStart.atStartOfDay())
                : mapper.sumMySignAmount(tenantId, myName, prevMonthStart.atStartOfDay(), monthStart.atStartOfDay());
        BigDecimal yoyAmount = scope == Scope.TEAM
                ? mapper.sumTeamSignAmount(tenantId, usernames, yoyMonthStart.atStartOfDay(), yoyMonthStart.plusMonths(1).atStartOfDay())
                : mapper.sumMySignAmount(tenantId, myName, yoyMonthStart.atStartOfDay(), yoyMonthStart.plusMonths(1).atStartOfDay());
        p.setCurrentMonthAmountGrowth(growthPct(monthAmount, prevAmount));
        p.setCurrentMonthAmountYoyGrowth(growthPct(monthAmount, yoyAmount));
        return p;
    }

    // ─── 明细弹窗（byMember） ─────────────────────────────────────────
    public List<Map<String, Object>> drillSign(LoginUser user,
                                               PeriodType type, LocalDate start, LocalDate end) {
        Range r = resolveRange(type, start, end);
        return mapper.rankSignCount(user.getTenantId(), r.start, r.end);
    }

    public List<Map<String, Object>> drillLead(LoginUser user, PeriodType type,
                                               LocalDate start, LocalDate end) {
        Range r = resolveRange(type, start, end);
        List<String> usernames = resolveUsernames(user, Scope.TEAM);
        int noFollowDays = bizConfigService.getGroupInt("lead.recycle", "no_follow_days", 14);
        int from = Math.max(noFollowDays - WARN_DAYS, 0);
        // 销售部成员：与 LeadAutoInvalidateTask 对齐的 ⑥ 指定人员过滤
        return mapper.leadStatsByMember(user.getTenantId(), usernames, r.start, r.end, from, noFollowDays, resolveSalesDeptUsernames());
    }

    public List<Map<String, Object>> drillCustomer(LoginUser user, PeriodType type,
                                                   LocalDate start, LocalDate end) {
        Range r = resolveRange(type, start, end);
        List<String> usernames = resolveUsernames(user, Scope.TEAM);
        int noFollowDays = bizConfigService.getGroupInt("customer.recycle", "no_follow_days", 30);
        int from = Math.max(noFollowDays - WARN_DAYS, 0);
        return mapper.customerStatsByMember(user.getTenantId(), usernames, r.start, r.end, from, noFollowDays);
    }

    // ─── 工具 ─────────────────────────────────────────────────────────
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
    private static String realNameOrUsername(Map<String, Object> row) {
        String r = asString(row.get("real_name"));
        return r != null && !r.isEmpty() ? r : asString(row.get("username"));
    }
    private static BigDecimal percent(long n, long d) {
        if (d <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(n).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(d), 2, RoundingMode.HALF_UP);
    }
    private static BigDecimal growthPct(long cur, long prev) {
        if (prev <= 0) return cur > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        return BigDecimal.valueOf(cur - prev).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(prev), 2, RoundingMode.HALF_UP);
    }
    private static BigDecimal growthPct(double cur, double prev) {
        if (prev <= 0) return cur > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        return BigDecimal.valueOf(cur - prev).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(prev), 2, RoundingMode.HALF_UP);
    }
    private static BigDecimal growthPct(BigDecimal cur, BigDecimal prev) {
        if (cur == null) cur = BigDecimal.ZERO;
        if (prev == null || prev.signum() <= 0) return cur.signum() > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        return cur.subtract(prev).multiply(BigDecimal.valueOf(100))
                .divide(prev, 2, RoundingMode.HALF_UP);
    }
}
