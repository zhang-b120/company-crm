package com.qykj.finmaster.workbench.service;

import com.qykj.finmaster.analytics.common.service.AnalyticsScopeResolver;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.workbench.dto.CsWorkbenchResp;
import com.qykj.finmaster.workbench.mapper.CsWorkbenchMapper;
import com.qykj.finmaster.workbench.util.PeriodCompare;
import com.qykj.finmaster.workbench.util.WorkingHoursCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 客服工作台个性化数据
 *
 * 数据源：work_order（task_type=BOOKKEEPING / RENEWAL，持有人=登录用户）
 * 数据收集模块尚未开发，前端固定显示占位提示
 */
@Service
public class CsWorkbenchService {

    /** "超 N 天未动" 阈值（业务确认 3 天） */
    private static final int STALE_DAYS = 3;

    @Autowired
    private CsWorkbenchMapper mapper;

    @Autowired
    private com.qykj.finmaster.system.user.mapper.UserMapper userMapper;

    @Autowired
    private AnalyticsScopeResolver scopeResolver;

    public enum Scope { PERSONAL, TEAM }

    /** 旧签名：保留兼容（默认 PERSONAL） */
    public CsWorkbenchResp summary(LoginUser user, String period) {
        return summary(user, Scope.PERSONAL, period);
    }

    public CsWorkbenchResp summary(LoginUser user, Scope scope, String period) {
        CsWorkbenchResp resp = new CsWorkbenchResp();
        if (user == null || user.getTenantId() == null || user.getUsername() == null) {
            return resp;
        }
        if (scope == null) scope = Scope.PERSONAL;
        resp.setScope(scope.name());
        Long tenantId = user.getTenantId();
        String username = user.getUsername();
        java.util.List<String> usernames = resolveUsernames(user, scope);
        java.util.List<Long> userIds = resolveUserIds(user, scope);

        LocalDate today = LocalDate.now();
        Date dayStart = toDate(today);
        Date dayEnd = toDate(today.plusDays(1));
        Date monthStart = toDate(today.withDayOfMonth(1));
        Date monthEnd = toDate(today.withDayOfMonth(1).plusMonths(1));

        resp.setBookkeeping(buildBookkeeping(tenantId, username, usernames, scope, dayStart, dayEnd, monthStart, monthEnd));
        resp.setRenewal(buildRenewal(tenantId, username, usernames, scope, dayStart, monthStart, monthEnd));
        resp.setDataCollection(buildDataCollection(tenantId, username, usernames, scope, resolvePeriod(period)));
        resp.setFieldWork(buildFieldWork(tenantId, user.getUserId(), userIds, scope, monthStart, monthEnd));
        return resp;
    }

    private java.util.List<String> resolveUsernames(LoginUser user, Scope scope) {
        if (scope == Scope.TEAM) {
            AnalyticsScopeResolver.ScopeResult res = scopeResolver.resolveTeamUsers(user);
            java.util.List<String> names = res.getUserNameList();
            if (names == null || names.isEmpty()) return java.util.Collections.singletonList(user.getUsername());
            return names;
        }
        return java.util.Collections.singletonList(user.getUsername());
    }

    private java.util.List<Long> resolveUserIds(LoginUser user, Scope scope) {
        if (scope == Scope.TEAM) {
            AnalyticsScopeResolver.ScopeResult res = scopeResolver.resolveTeamUsers(user);
            java.util.List<Long> ids = res.getUserIdList();
            if (ids == null || ids.isEmpty())
                return user.getUserId() == null ? java.util.Collections.emptyList()
                        : java.util.Collections.singletonList(user.getUserId());
            return ids;
        }
        return user.getUserId() == null ? java.util.Collections.emptyList()
                : java.util.Collections.singletonList(user.getUserId());
    }

    /** 默认账期：15 号前 = 上月 1 号；15 号及以后 = 当月 1 号。与公司列表 / BankStatementService 保持一致 */
    private static String resolvePeriod(String period) {
        if (period != null && !period.isEmpty()) return period;
        LocalDate today = LocalDate.now();
        LocalDate p = today.getDayOfMonth() < 15
                ? today.minusMonths(1).withDayOfMonth(1)
                : today.withDayOfMonth(1);
        return p.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 外勤任务（field_work_task），支持 PERSONAL / TEAM 视角
     */
    private CsWorkbenchResp.TaskGroup buildFieldWork(
            Long tenantId, Long userId, java.util.List<Long> userIds, Scope scope,
            Date monthStart, Date monthEnd) {
        if (scope == Scope.PERSONAL) {
            if (userId == null) return null;
            long all = nvl(mapper.countMyFieldWorkAllTasks(userId));
            if (all == 0) return null;
        } else {
            if (userIds == null || userIds.isEmpty()) return null;
            long all = nvl(mapper.countTeamFieldWorkAll(userIds));
            if (all == 0) return null;
        }

        long total, monthDone, staleCount, monthNew;
        if (scope == Scope.TEAM) {
            total = nvl(mapper.countTeamFieldWorkActive(userIds));
            staleCount = nvl(mapper.countTeamFieldWorkStale(userIds, STALE_DAYS));
            monthDone = nvl(mapper.countTeamFieldWorkMonthDone(userIds, monthStart, monthEnd));
            monthNew = nvl(mapper.countTeamFieldWorkMonthNew(userIds, monthStart, monthEnd));
        } else {
            total = nvl(mapper.countMyFieldWorkActiveTasks(userId));
            staleCount = nvl(mapper.countMyFieldWorkStaleTasks(userId, STALE_DAYS));
            monthDone = nvl(mapper.countMyFieldWorkMonthDone(userId, monthStart, monthEnd));
            monthNew = nvl(mapper.countMyFieldWorkMonthNew(userId, monthStart, monthEnd));
        }
        CsWorkbenchResp.TaskGroup g = new CsWorkbenchResp.TaskGroup();
        g.setTotal(total);
        g.setStaleCount(staleCount);
        g.setMonthDone(monthDone);
        g.setMonthNew(monthNew);
        g.setMonthCompletionRate(percent(monthDone, monthNew));

        // 同环比
        Date prevStart = toDate(toLocalDate(monthStart).minusMonths(1));
        Date prevEnd = monthStart;
        Date yoyStart = toDate(toLocalDate(monthStart).minusYears(1));
        Date yoyEnd = toDate(toLocalDate(monthEnd).minusYears(1));
        long prevDone = scope == Scope.TEAM
                ? nvl(mapper.countTeamFieldWorkMonthDone(userIds, prevStart, prevEnd))
                : nvl(mapper.countMyFieldWorkMonthDone(userId, prevStart, prevEnd));
        long yoyDone = scope == Scope.TEAM
                ? nvl(mapper.countTeamFieldWorkMonthDone(userIds, yoyStart, yoyEnd))
                : nvl(mapper.countMyFieldWorkMonthDone(userId, yoyStart, yoyEnd));
        long prevNew = scope == Scope.TEAM
                ? nvl(mapper.countTeamFieldWorkMonthNew(userIds, prevStart, prevEnd))
                : nvl(mapper.countMyFieldWorkMonthNew(userId, prevStart, prevEnd));
        long yoyNew = scope == Scope.TEAM
                ? nvl(mapper.countTeamFieldWorkMonthNew(userIds, yoyStart, yoyEnd))
                : nvl(mapper.countMyFieldWorkMonthNew(userId, yoyStart, yoyEnd));
        g.setMonthDoneGrowth(growthPct(monthDone, prevDone));
        g.setMonthDoneYoyGrowth(growthPct(monthDone, yoyDone));
        g.setMonthNewGrowth(growthPct(monthNew, prevNew));
        g.setMonthNewYoyGrowth(growthPct(monthNew, yoyNew));
        g.setTotalGrowth(BigDecimal.ZERO);
        g.setStaleCountGrowth(BigDecimal.ZERO);

        g.setMonthAvgWorkingHours(scope == Scope.TEAM
                ? teamFieldWorkAvgHours(userIds, monthStart, monthEnd)
                : fieldWorkAvgHours(userId, monthStart, monthEnd));
        g.setMonthRanking(buildFieldWorkRanking(tenantId, userId, monthStart, monthEnd));
        return g;
    }

    private BigDecimal teamFieldWorkAvgHours(java.util.List<Long> userIds, Date monthStart, Date monthEnd) {
        List<Map<String, Object>> rows = mapper.selectTeamFieldWorkMonthCompletedTimes(userIds, monthStart, monthEnd);
        return averageHours(rows);
    }
    private BigDecimal averageHours(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return BigDecimal.ZERO;
        long totalMinutes = 0L;
        int count = 0;
        for (Map<String, Object> row : rows) {
            LocalDateTime created = toLdt(row.get("createTime"));
            LocalDateTime completed = toLdt(row.get("completeTime"));
            if (created == null || completed == null) continue;
            totalMinutes += WorkingHoursCalculator.minutesBetween(created, completed);
            count++;
        }
        if (count == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** 外勤平均工作时长：(actual_end_time - create_time) 按工作日 8:15-12:00 + 14:00-18:00 计算 */
    private BigDecimal fieldWorkAvgHours(Long userId, Date monthStart, Date monthEnd) {
        List<Map<String, Object>> rows = mapper.selectMyFieldWorkMonthCompletedTimes(userId, monthStart, monthEnd);
        if (rows == null || rows.isEmpty()) return BigDecimal.ZERO;
        long totalMinutes = 0L;
        int count = 0;
        for (Map<String, Object> row : rows) {
            java.time.LocalDateTime created = toLdt(row.get("createTime"));
            java.time.LocalDateTime completed = toLdt(row.get("completeTime"));
            if (created == null || completed == null) continue;
            totalMinutes += WorkingHoursCalculator.minutesBetween(created, completed);
            count++;
        }
        if (count == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(totalMinutes)
                .divide(BigDecimal.valueOf(count), 4, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** 外勤排名：本月 COMPLETED 数 group by assignee_id，需要把当前用户 userId 翻译为 username 才能匹配 */
    private CsWorkbenchResp.RankingInfo buildFieldWorkRanking(Long tenantId, Long userId, Date monthStart, Date monthEnd) {
        List<Map<String, Object>> rows = mapper.rankFieldWorkMonthCompleted(tenantId, monthStart, monthEnd);
        // 排名行的 username 来自 sys_user.username，找出当前用户对应的 username 用于定位"我"
        String myUsername = null;
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                // sys_user 主键 id 与外部传的 userId 比对：rank SQL 里我们没返回 user.id，
                // 但 sys_user.username 是唯一的；这里通过下游 buildRankingFromRows 用 username 比对
                // 因此我们需要先用 userId → username。
                // 简化：跳过该步，直接由 service 调用方传 username
            }
        }
        // 改走外部已知的 username（由调用方传入更直接）；这里 fallback 用 buildRankingFromRows 时传 myUsername
        // 因复杂度，直接调用 buildRankingFromRows 传入 user 的 username 副本：从 rows 中找 id-匹配行更简单。
        return buildRankingFromRows(rows, lookupUsernameByUserId(rows, userId));
    }

    /**
     * 在排行结果中查找 userId 对应的 username（rank SQL 已 JOIN sys_user 但只 SELECT 了 username）
     * 由于排行 SQL 没返回 id，这里依赖外部把 userId → username 翻译
     * fallback：返回一个不会匹配任何 row 的占位字符串，使得"我"判定为未上榜
     */
    private String lookupUsernameByUserId(List<Map<String, Object>> rows, Long userId) {
        // rows 中没有 id 字段。改造方式：在 rank SQL 里多 SELECT 一列 user_id 即可。
        // 这里走捷径：补一次轻量 lookup —— 通过 rows.username 与 userId 在 sys_user 中匹配。
        // 为不引入 UserMapper 依赖，用 userMapper.selectById(userId) 拿 username。
        return userIdToUsername(userId);
    }

    private String userIdToUsername(Long userId) {
        if (userId == null) return "__nobody__";
        com.qykj.finmaster.system.user.entity.User u = userMapper.selectById(userId);
        return u != null && u.getUsername() != null ? u.getUsername() : "__nobody__";
    }

    /**
     * 数据收集
     * - PERSONAL: 仅本人
     * - TEAM: 团队所有 butler 的合计
     */
    private CsWorkbenchResp.DataCollectionGroup buildDataCollection(
            Long tenantId, String username, java.util.List<String> usernames, Scope scope, String period) {
        if (scope == Scope.PERSONAL) {
            long taskCount = nvl(mapper.countMyDataCollectionTasksByPeriod(tenantId, username, period));
            if (taskCount == 0) return null;
            long total = nvl(mapper.countCompaniesByButler(tenantId, username));
            if (total == 0) return null;
            long done = nvl(mapper.countCompaniesUploadedByPeriod(tenantId, username, period));
            CsWorkbenchResp.DataCollectionGroup g = new CsWorkbenchResp.DataCollectionGroup();
            g.setPeriod(period);
            g.setTotal(total);
            g.setDone(done);
            g.setMonthDone(done);
            g.setTotalDone(done);
            g.setCompletionRate(percent(done, total));
            g.setMonthRanking(buildPeriodEnterpriseRanking(tenantId, username, period));
            return g;
        }
        // TEAM: 把团队成员的每人数据汇总
        java.util.List<Map<String, Object>> rows = mapper.dataCollectionStatsByMember(tenantId, usernames, period);
        if (rows == null || rows.isEmpty()) return null;
        long total = 0L, done = 0L;
        for (Map<String, Object> r : rows) {
            total += asLong(r.get("total_count"));
            done += asLong(r.get("done_count"));
        }
        if (total == 0) return null;
        CsWorkbenchResp.DataCollectionGroup g = new CsWorkbenchResp.DataCollectionGroup();
        g.setPeriod(period);
        g.setTotal(total);
        g.setDone(done);
        g.setMonthDone(done);
        g.setTotalDone(done);
        g.setCompletionRate(percent(done, total));
        g.setMonthRanking(buildPeriodEnterpriseRanking(tenantId, username, period));
        return g;
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    /** 数据收集排名：所有 butler 在指定账期下的企业完成率倒序 */
    private CsWorkbenchResp.RankingInfo buildPeriodEnterpriseRanking(Long tenantId, String username, String period) {
        CsWorkbenchResp.RankingInfo r = new CsWorkbenchResp.RankingInfo();
        r.setMetric("RATE");
        List<Map<String, Object>> rows = mapper.rankDataCollectionByPeriod(tenantId, period);
        if (rows == null) rows = java.util.Collections.emptyList();

        BigDecimal myRate = BigDecimal.ZERO;
        Integer myIdx = null;
        for (int i = 0; i < rows.size(); i++) {
            if (username.equals(rows.get(i).get("username"))) {
                myRate = asBd(rows.get(i).get("rate"));
                myIdx = i;
                break;
            }
        }
        r.setMyRate(myRate);
        r.setTotalRanked(rows.size());

        Integer rank;
        if (myIdx != null) rank = myIdx + 1;
        else if (myRate.signum() > 0) rank = rows.size() + 1;
        else rank = null;
        r.setMyRank(rank);

        if (!rows.isEmpty()) {
            BigDecimal topRate = asBd(rows.get(0).get("rate"));
            r.setTopRate(topRate);
            r.setTopName(realNameOrUsername(rows.get(0)));
            r.setDiffToTopRate(maxBd(topRate.subtract(myRate)));
        } else {
            r.setTopRate(BigDecimal.ZERO);
            r.setDiffToTopRate(BigDecimal.ZERO);
        }
        if (myIdx != null) {
            if (myIdx > 0) {
                BigDecimal prevRate = asBd(rows.get(myIdx - 1).get("rate"));
                r.setPrevName(realNameOrUsername(rows.get(myIdx - 1)));
                r.setDiffToPrevRate(maxBd(prevRate.subtract(myRate)));
            } else {
                r.setDiffToPrevRate(BigDecimal.ZERO);
            }
            if (myIdx < rows.size() - 1) {
                BigDecimal nextRate = asBd(rows.get(myIdx + 1).get("rate"));
                r.setNextName(realNameOrUsername(rows.get(myIdx + 1)));
                r.setLeadOverNextRate(maxBd(myRate.subtract(nextRate)));
            }
        }
        return r;
    }

    private CsWorkbenchResp.TaskGroup buildBookkeeping(
            Long tenantId, String username, java.util.List<String> usernames, Scope scope,
            Date dayStart, Date dayEnd, Date monthStart, Date monthEnd) {
        CsWorkbenchResp.TaskGroup g = new CsWorkbenchResp.TaskGroup();
        // 当前
        long total, monthDone, staleCount, monthNew;
        if (scope == Scope.TEAM) {
            total = nvl(mapper.countTeamActiveTasks(tenantId, usernames, "BOOKKEEPING"));
            staleCount = nvl(mapper.countTeamStaleTasks(tenantId, usernames, "BOOKKEEPING", STALE_DAYS));
            monthDone = nvl(mapper.countTeamMonthDone(tenantId, usernames, "BOOKKEEPING", monthStart, monthEnd));
            monthNew = nvl(mapper.countTeamMonthNew(tenantId, usernames, "BOOKKEEPING",
                    toLdt(monthStart), toLdt(monthEnd)));
        } else {
            total = nvl(mapper.countMyActiveTasks(tenantId, username, "BOOKKEEPING"));
            staleCount = nvl(mapper.countMyStaleTasks(tenantId, username, "BOOKKEEPING", STALE_DAYS));
            monthDone = nvl(mapper.countMyMonthDone(tenantId, username, "BOOKKEEPING", monthStart, monthEnd));
            monthNew = nvl(mapper.countMyMonthNew(tenantId, username, "BOOKKEEPING",
                    toLdt(monthStart), toLdt(monthEnd)));
        }
        g.setTotal(total);
        g.setStaleCount(staleCount);
        g.setMonthDone(monthDone);
        g.setMonthNew(monthNew);
        g.setMonthCompletionRate(percent(monthDone, monthNew));
        g.setTodayDue(scope == Scope.TEAM ? 0L
                : nvl(mapper.countMyTodayDue(tenantId, username, "BOOKKEEPING", dayStart, dayEnd)));

        // 同环比（按月：上月 vs 去年同月）
        Date prevStart = toDate(toLocalDate(monthStart).minusMonths(1));
        Date prevEnd = monthStart;
        Date yoyStart = toDate(toLocalDate(monthStart).minusYears(1));
        Date yoyEnd = toDate(toLocalDate(monthEnd).minusYears(1));
        long prevDone = scope == Scope.TEAM
                ? nvl(mapper.countTeamMonthDone(tenantId, usernames, "BOOKKEEPING", prevStart, prevEnd))
                : nvl(mapper.countMyMonthDone(tenantId, username, "BOOKKEEPING", prevStart, prevEnd));
        long yoyDone = scope == Scope.TEAM
                ? nvl(mapper.countTeamMonthDone(tenantId, usernames, "BOOKKEEPING", yoyStart, yoyEnd))
                : nvl(mapper.countMyMonthDone(tenantId, username, "BOOKKEEPING", yoyStart, yoyEnd));
        long prevNew = scope == Scope.TEAM
                ? nvl(mapper.countTeamMonthNew(tenantId, usernames, "BOOKKEEPING", toLdt(prevStart), toLdt(prevEnd)))
                : nvl(mapper.countMyMonthNew(tenantId, username, "BOOKKEEPING", toLdt(prevStart), toLdt(prevEnd)));
        long yoyNew = scope == Scope.TEAM
                ? nvl(mapper.countTeamMonthNew(tenantId, usernames, "BOOKKEEPING", toLdt(yoyStart), toLdt(yoyEnd)))
                : nvl(mapper.countMyMonthNew(tenantId, username, "BOOKKEEPING", toLdt(yoyStart), toLdt(yoyEnd)));
        g.setMonthDoneGrowth(growthPct(monthDone, prevDone));
        g.setMonthDoneYoyGrowth(growthPct(monthDone, yoyDone));
        g.setMonthNewGrowth(growthPct(monthNew, prevNew));
        g.setMonthNewYoyGrowth(growthPct(monthNew, yoyNew));
        g.setTotalGrowth(BigDecimal.ZERO);  // 待处理任务为快照，不做同环比
        g.setStaleCountGrowth(BigDecimal.ZERO);

        g.setMonthAvgWorkingHours(avgWorkingHours(tenantId, username, usernames, scope, "BOOKKEEPING", monthStart, monthEnd));
        // 排名仍是个人视角（团队长看自己的位次）
        g.setMonthRanking(buildRankingByStep(tenantId, username, "BOOKKEEPING", "HANDYMAN_FILL", monthStart, monthEnd));
        return g;
    }

    private CsWorkbenchResp.RenewalGroup buildRenewal(
            Long tenantId, String username, java.util.List<String> usernames, Scope scope,
            Date now, Date monthStart, Date monthEnd) {
        CsWorkbenchResp.RenewalGroup g = new CsWorkbenchResp.RenewalGroup();
        long total, monthDue, overdue, monthDone;
        BigDecimal amount;
        if (scope == Scope.TEAM) {
            total = nvl(mapper.countTeamActiveTasks(tenantId, usernames, "RENEWAL"));
            monthDue = nvl(mapper.countTeamMonthDue(tenantId, usernames, "RENEWAL", monthStart, monthEnd));
            overdue = nvl(mapper.countTeamOverdue(tenantId, usernames, "RENEWAL", now));
            monthDone = nvl(mapper.countTeamMonthDone(tenantId, usernames, "RENEWAL", monthStart, monthEnd));
            amount = mapper.sumTeamMonthRenewAmount(tenantId, usernames, monthStart, monthEnd);
        } else {
            total = nvl(mapper.countMyActiveTasks(tenantId, username, "RENEWAL"));
            monthDue = nvl(mapper.countMyMonthDue(tenantId, username, "RENEWAL", monthStart, monthEnd));
            overdue = nvl(mapper.countMyOverdue(tenantId, username, "RENEWAL", now));
            monthDone = nvl(mapper.countMyMonthDone(tenantId, username, "RENEWAL", monthStart, monthEnd));
            amount = mapper.sumMyMonthRenewAmount(tenantId, username, monthStart, monthEnd);
        }
        g.setTotal(total);
        g.setMonthDue(monthDue);
        g.setOverdue(overdue);
        g.setMonthDone(monthDone);
        g.setMonthRenewalRate(percent(monthDone, monthDue));
        g.setMonthRenewAmount(amount == null ? BigDecimal.ZERO : amount);

        // 同环比
        Date prevStart = toDate(toLocalDate(monthStart).minusMonths(1));
        Date prevEnd = monthStart;
        Date yoyStart = toDate(toLocalDate(monthStart).minusYears(1));
        Date yoyEnd = toDate(toLocalDate(monthEnd).minusYears(1));
        long prevDone = scope == Scope.TEAM
                ? nvl(mapper.countTeamMonthDone(tenantId, usernames, "RENEWAL", prevStart, prevEnd))
                : nvl(mapper.countMyMonthDone(tenantId, username, "RENEWAL", prevStart, prevEnd));
        long yoyDone = scope == Scope.TEAM
                ? nvl(mapper.countTeamMonthDone(tenantId, usernames, "RENEWAL", yoyStart, yoyEnd))
                : nvl(mapper.countMyMonthDone(tenantId, username, "RENEWAL", yoyStart, yoyEnd));
        BigDecimal prevAmount = scope == Scope.TEAM
                ? mapper.sumTeamMonthRenewAmount(tenantId, usernames, prevStart, prevEnd)
                : mapper.sumMyMonthRenewAmount(tenantId, username, prevStart, prevEnd);
        BigDecimal yoyAmount = scope == Scope.TEAM
                ? mapper.sumTeamMonthRenewAmount(tenantId, usernames, yoyStart, yoyEnd)
                : mapper.sumMyMonthRenewAmount(tenantId, username, yoyStart, yoyEnd);
        g.setMonthDoneGrowth(growthPct(monthDone, prevDone));
        g.setMonthDoneYoyGrowth(growthPct(monthDone, yoyDone));
        g.setMonthRenewAmountGrowth(growthPct(amount, prevAmount));
        g.setMonthRenewAmountYoyGrowth(growthPct(amount, yoyAmount));
        g.setTotalGrowth(BigDecimal.ZERO);
        g.setOverdueGrowth(BigDecimal.ZERO);

        g.setMonthAvgWorkingHours(avgWorkingHours(tenantId, username, usernames, scope, "RENEWAL", monthStart, monthEnd));
        g.setMonthRanking(buildRanking(tenantId, username, "RENEWAL", monthStart, monthEnd));
        return g;
    }

    /**
     * 本月完成数排名（按整个工单完成口径）：所有同类型任务持有人中按 DONE 数量倒序
     */
    private CsWorkbenchResp.RankingInfo buildRanking(Long tenantId, String username, String taskType,
                                                     Date monthStart, Date monthEnd) {
        List<Map<String, Object>> rows = mapper.rankMonthCompletedByOwner(tenantId, taskType, monthStart, monthEnd);
        return buildRankingFromRows(rows, username);
    }

    /**
     * 本月节点完成数排名：按 work_order_step 中指定 step_code 完成数倒序
     */
    private CsWorkbenchResp.RankingInfo buildRankingByStep(Long tenantId, String username, String taskType,
                                                            String stepCode, Date monthStart, Date monthEnd) {
        List<Map<String, Object>> rows = mapper.rankMonthCompletedByStep(tenantId, taskType, stepCode, monthStart, monthEnd);
        return buildRankingFromRows(rows, username);
    }

    /**
     * 按完成率排名：排序在 SQL 端完成（rate desc，done_count desc）
     */
    private CsWorkbenchResp.RankingInfo buildRankingByRate(Long tenantId, String username, String taskType) {
        CsWorkbenchResp.RankingInfo r = new CsWorkbenchResp.RankingInfo();
        r.setMetric("RATE");
        List<Map<String, Object>> rows = mapper.rankByCompletionRate(tenantId, taskType);
        if (rows == null) rows = java.util.Collections.emptyList();

        BigDecimal myRate = BigDecimal.ZERO;
        Integer myIdx = null;
        for (int i = 0; i < rows.size(); i++) {
            if (username.equals(rows.get(i).get("username"))) {
                myRate = asBd(rows.get(i).get("rate"));
                myIdx = i;
                break;
            }
        }
        r.setMyRate(myRate);
        r.setTotalRanked(rows.size());

        Integer rank;
        if (myIdx != null) rank = myIdx + 1;
        else if (myRate.signum() > 0) rank = rows.size() + 1;
        else rank = null;
        r.setMyRank(rank);

        if (!rows.isEmpty()) {
            BigDecimal topRate = asBd(rows.get(0).get("rate"));
            r.setTopRate(topRate);
            r.setTopName(realNameOrUsername(rows.get(0)));
            r.setDiffToTopRate(maxBd(topRate.subtract(myRate)));
        } else {
            r.setTopRate(BigDecimal.ZERO);
            r.setDiffToTopRate(BigDecimal.ZERO);
        }
        if (myIdx != null) {
            if (myIdx > 0) {
                BigDecimal prevRate = asBd(rows.get(myIdx - 1).get("rate"));
                r.setPrevName(realNameOrUsername(rows.get(myIdx - 1)));
                r.setDiffToPrevRate(maxBd(prevRate.subtract(myRate)));
            } else {
                r.setDiffToPrevRate(BigDecimal.ZERO);
            }
            if (myIdx < rows.size() - 1) {
                BigDecimal nextRate = asBd(rows.get(myIdx + 1).get("rate"));
                r.setNextName(realNameOrUsername(rows.get(myIdx + 1)));
                r.setLeadOverNextRate(maxBd(myRate.subtract(nextRate)));
            }
        }
        return r;
    }

    private static BigDecimal asBd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(o.toString());
        try { return new BigDecimal(o.toString()); } catch (Exception e) { return BigDecimal.ZERO; }
    }
    private static BigDecimal maxBd(BigDecimal v) {
        return v == null || v.signum() < 0 ? BigDecimal.ZERO : v;
    }

    private CsWorkbenchResp.RankingInfo buildRankingFromRows(List<Map<String, Object>> rows, String username) {
        CsWorkbenchResp.RankingInfo r = new CsWorkbenchResp.RankingInfo();
        r.setMetric("COUNT");
        if (rows == null) rows = java.util.Collections.emptyList();

        long myCount = 0L;
        Integer myIdx = null;
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            if (username.equals(row.get("username"))) {
                myCount = asLong(row.get("count"));
                myIdx = i;
                break;
            }
        }
        r.setMyCount(myCount);
        r.setTotalRanked(rows.size());
        // 显式分支避免 ternary 中 (Integer/null) 自动拆箱 NPE
        Integer rank;
        if (myIdx != null) rank = myIdx + 1;
        else if (myCount > 0) rank = rows.size() + 1;
        else rank = null;
        r.setMyRank(rank);

        if (!rows.isEmpty()) {
            Map<String, Object> top = rows.get(0);
            r.setTopName(realNameOrUsername(top));
            long topCount = asLong(top.get("count"));
            r.setDiffToTop(Math.max(topCount - myCount, 0));
        } else {
            r.setDiffToTop(0L);
        }

        if (myIdx != null) {
            if (myIdx > 0) {
                Map<String, Object> prev = rows.get(myIdx - 1);
                r.setPrevName(realNameOrUsername(prev));
                r.setDiffToPrev(Math.max(asLong(prev.get("count")) - myCount, 0));
            } else {
                r.setDiffToPrev(0L);
            }
            if (myIdx < rows.size() - 1) {
                Map<String, Object> next = rows.get(myIdx + 1);
                r.setNextName(realNameOrUsername(next));
                r.setLeadOverNext(Math.max(myCount - asLong(next.get("count")), 0));
            }
        }
        return r;
    }

    private static long asLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString()); } catch (Exception e) { return 0L; }
    }

    private static String realNameOrUsername(Map<String, Object> row) {
        Object r = row.get("real_name");
        String real = r == null ? null : r.toString();
        if (real != null && !real.isEmpty()) return real;
        Object u = row.get("username");
        return u == null ? null : u.toString();
    }

    /** 平均工作时长（PERSONAL / TEAM 自动选择） */
    private BigDecimal avgWorkingHours(Long tenantId, String username, java.util.List<String> usernames,
                                        Scope scope, String taskType, Date monthStart, Date monthEnd) {
        List<Map<String, Object>> rows = scope == Scope.TEAM
                ? mapper.selectTeamMonthCompletedTaskTimes(tenantId, usernames, taskType, monthStart, monthEnd)
                : mapper.selectMyMonthCompletedTaskTimes(tenantId, username, taskType, monthStart, monthEnd);
        return averageHours(rows);
    }

    /** Date → LocalDate */
    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Date → LocalDateTime */
    private static LocalDateTime toLdt(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static BigDecimal growthPct(long cur, long prev) {
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

    // ============= 明细查询（drill） =============

    public List<Map<String, Object>> drillBookkeeping(LoginUser user, Date monthStart, Date monthEnd) {
        java.util.List<String> usernames = resolveUsernames(user, Scope.TEAM);
        return mapper.bookkeepingStatsByMember(user.getTenantId(), usernames, monthStart, monthEnd, STALE_DAYS);
    }

    public List<Map<String, Object>> drillRenewal(LoginUser user, Date monthStart, Date monthEnd, Date now) {
        java.util.List<String> usernames = resolveUsernames(user, Scope.TEAM);
        return mapper.renewalStatsByMember(user.getTenantId(), usernames, monthStart, monthEnd, now);
    }

    public List<Map<String, Object>> drillDataCollection(LoginUser user, String period) {
        java.util.List<String> usernames = resolveUsernames(user, Scope.TEAM);
        return mapper.dataCollectionStatsByMember(user.getTenantId(), usernames, resolvePeriod(period));
    }

    public List<Map<String, Object>> drillFieldWork(LoginUser user, Date monthStart, Date monthEnd) {
        java.util.List<Long> userIds = resolveUserIds(user, Scope.TEAM);
        return mapper.fieldWorkStatsByMember(user.getTenantId(), userIds, monthStart, monthEnd, STALE_DAYS);
    }

    private static LocalDateTime toLdt(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime) return (LocalDateTime) o;
        if (o instanceof Timestamp) return ((Timestamp) o).toLocalDateTime();
        if (o instanceof Date) return ((Date) o).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return null;
    }

    private static long nvl(Long v) { return v == null ? 0L : v; }

    private static BigDecimal percent(long numerator, long denominator) {
        if (denominator <= 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(numerator).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
