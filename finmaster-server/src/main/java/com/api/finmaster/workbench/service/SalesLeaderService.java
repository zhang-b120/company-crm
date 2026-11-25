package com.qykj.finmaster.workbench.service;

import com.qykj.finmaster.analytics.common.service.AnalyticsScopeResolver;
import com.qykj.finmaster.customer.lead.constants.LeadRecycleConstants;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.mapper.UserDepartmentMapper;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import com.qykj.finmaster.workbench.dto.SalesLeaderResp;
import com.qykj.finmaster.workbench.mapper.SalesLeaderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** 销售团队长工作台聚合 */
@Service
public class SalesLeaderService {

    @Autowired
    private SalesLeaderMapper mapper;
    @Autowired
    private AnalyticsScopeResolver scopeResolver;
    @Autowired
    private SysBizConfigService bizConfigService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserDepartmentMapper userDepartmentMapper;

    private static final int WARN_DAYS = 3;
    private static final int TOP_LIMIT = 5;
    /** 涨跌榜门槛：上月至少签单 N 单才入榜，避免低基数噪声 */
    private static final long DELTA_PREV_MIN_COUNT = 10;

    /** 销售部（含子部门）成员 username 列表：用于"即将回收"等与 LeadAutoInvalidateTask 对齐的过滤 */
    private List<String> resolveSalesDeptUsernames() {
        List<User> users = userMapper.selectUsersByDeptWithChildren(LeadRecycleConstants.SALES_DEPT_ID, null);
        if (users == null || users.isEmpty()) return Collections.emptyList();
        Set<String> names = new LinkedHashSet<>();
        for (User u : users) {
            if (u != null && u.getUsername() != null && !u.getUsername().isEmpty()) {
                names.add(u.getUsername());
            }
        }
        return new ArrayList<>(names);
    }

    /** 取登录账号所在部门（含子部门）下的全部成员用户名（不依赖 leader_username）*/
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

    public SalesLeaderResp summary(LoginUser user) {
        SalesLeaderResp resp = new SalesLeaderResp();
        if (user == null || user.getTenantId() == null) return resp;
        Long tenantId = user.getTenantId();

        // 数据范围 + 诊断下拉统一按"登录账号所在部门"展开（不依赖 leader_username）
        List<String> usernames = resolveScopeUsernames(user);
        resp.setMembers(buildMemberOptions(tenantId, usernames));

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime mStart = monthStart.atStartOfDay();
        LocalDateTime mEnd = monthStart.plusMonths(1).atStartOfDay();
        LocalDate prevMonthStart = monthStart.minusMonths(1);
        LocalDate yoyMonthStart = monthStart.minusYears(1);

        resp.setProgress(buildProgress(tenantId, usernames, dayStart, dayEnd, mStart, mEnd));
        resp.setTrend(buildTrend(tenantId, usernames, monthStart, prevMonthStart, yoyMonthStart, mStart, mEnd));
        return resp;
    }

    private List<SalesLeaderResp.MemberOption> buildMemberOptions(Long tenantId, List<String> usernames) {
        List<SalesLeaderResp.MemberOption> opts = new ArrayList<>();
        if (usernames == null || usernames.isEmpty()) return opts;
        // 走 sys_user 拉部门下全部成员（含没有任何业绩动作的人，诊断画像才能选到）
        List<Map<String, Object>> rows = mapper.listMembersByUsernames(tenantId, usernames);
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                SalesLeaderResp.MemberOption o = new SalesLeaderResp.MemberOption();
                o.setUsername(asString(r.get("username")));
                o.setRealName(asString(r.get("real_name")));
                opts.add(o);
            }
        }
        return opts;
    }

    private SalesLeaderResp.ProgressBlock buildProgress(Long tenantId, List<String> usernames,
                                                         LocalDateTime dayStart, LocalDateTime dayEnd,
                                                         LocalDateTime mStart, LocalDateTime mEnd) {
        SalesLeaderResp.ProgressBlock p = new SalesLeaderResp.ProgressBlock();

        // A1 拆线索 / 客户 / 异常 —— 详情列表改成懒加载（点击弹窗后再查）以加速首屏
        p.setTodayPendingLeadCount(nvl(mapper.countTodayPendingLead(tenantId, usernames, dayStart, dayEnd)));
        p.setTodayPendingCustomerCount(nvl(mapper.countTodayPendingCustomer(tenantId, usernames, dayStart, dayEnd)));
        p.setAbnormalSameDayCount(nvl(mapper.countAbnormalSameDay(tenantId, usernames)));
        p.setTodayPendingByMember(toMemberStats(mapper.todayPendingByMember(tenantId, usernames, dayStart, dayEnd)));

        // A2 即将回收 —— 仅总数 + byMember 在首屏；明细列表懒加载
        // 销售部成员列表：用于"即将回收"线索查询与 LeadAutoInvalidateTask 对齐（⑥ 指定人员）
        int leadDays = bizConfigService.getGroupInt("lead.recycle", "no_follow_days", 14);
        int custDays = bizConfigService.getGroupInt("customer.recycle", "no_follow_days", 30);
        int leadFrom = Math.max(leadDays - WARN_DAYS, 0);
        int custFrom = Math.max(custDays - WARN_DAYS, 0);
        List<String> salesDeptUsernames = resolveSalesDeptUsernames();
        p.setAboutRecycleLeadCount(nvl(mapper.countAboutRecycleLead(tenantId, usernames, leadFrom, leadDays, salesDeptUsernames)));
        p.setAboutRecycleCustomerCount(nvl(mapper.countAboutRecycleCustomer(tenantId, usernames, custFrom, custDays)));
        p.setAboutRecycleByMember(toMemberStats(mapper.aboutRecycleByMember(tenantId, usernames, leadFrom, leadDays, custFrom, custDays, salesDeptUsernames)));

        // A3 沉睡客户：三段总数 + 按成员明细
        Map<String, Object> seg = mapper.staleCustomerSegments(tenantId, usernames);
        if (seg != null) {
            p.setStaleCount7(asLong(seg.get("count7")));
            p.setStaleCount14(asLong(seg.get("count14")));
            p.setStaleCount30(asLong(seg.get("count30")));
        }
        List<SalesLeaderResp.StaleStat> stales = new ArrayList<>();
        List<Map<String, Object>> staleRows = mapper.staleCustomerByMember(tenantId, usernames);
        if (staleRows != null) {
            for (Map<String, Object> r : staleRows) {
                SalesLeaderResp.StaleStat s = new SalesLeaderResp.StaleStat();
                s.setUsername(asString(r.get("username")));
                s.setRealName(asString(r.get("real_name")));
                s.setCount7(asLong(r.get("count7")));
                s.setCount14(asLong(r.get("count14")));
                s.setCount30(asLong(r.get("count30")));
                stales.add(s);
            }
        }
        p.setStaleCustomerByMember(stales);

        // A4 今日 0 动作成员
        List<SalesLeaderResp.MemberAction> actions = new ArrayList<>();
        List<Map<String, Object>> actRows = mapper.memberDailyAction(tenantId, usernames, dayStart, dayEnd);
        long zeroCount = 0L;
        List<SalesLeaderResp.MemberAction> zeroList = new ArrayList<>();
        if (actRows != null) {
            for (Map<String, Object> r : actRows) {
                SalesLeaderResp.MemberAction a = new SalesLeaderResp.MemberAction();
                a.setUsername(asString(r.get("username")));
                a.setRealName(asString(r.get("real_name")));
                a.setTodayFollowCount(asLong(r.get("today_follow_count")));
                a.setTodayCallCount(asLong(r.get("today_call_count")));
                a.setTodayOrderCount(asLong(r.get("today_order_count")));
                actions.add(a);
                if (a.getTodayFollowCount() == 0 && a.getTodayCallCount() == 0 && a.getTodayOrderCount() == 0) {
                    zeroCount++;
                    zeroList.add(a);
                }
            }
        }
        p.setZeroActionMemberCount(zeroCount);
        p.setZeroActionMembers(zeroList);

        // A5 在途订单
        Map<String, Object> pendingAgg = mapper.pendingOrderAgg(tenantId, usernames);
        if (pendingAgg != null) {
            p.setPendingOrderAmount(asBd(pendingAgg.get("amount")));
            p.setPendingOrderCount(asLong(pendingAgg.get("count")));
        }
        List<SalesLeaderResp.MemberOrder> pendingMembers = new ArrayList<>();
        List<Map<String, Object>> pmRows = mapper.pendingOrderByMember(tenantId, usernames);
        if (pmRows != null) {
            for (Map<String, Object> r : pmRows) {
                SalesLeaderResp.MemberOrder m = new SalesLeaderResp.MemberOrder();
                m.setUsername(asString(r.get("username")));
                m.setRealName(asString(r.get("real_name")));
                m.setOrderCount(asLong(r.get("order_count")));
                m.setAmount(asBd(r.get("amount")));
                m.setAvgAgeDays(asBd(r.get("avg_age_days")));
                pendingMembers.add(m);
            }
        }
        p.setPendingOrderByMember(pendingMembers);

        // A6 退款
        Map<String, Object> refundAgg = mapper.refundAgg(tenantId, usernames, mStart, mEnd);
        if (refundAgg != null) {
            p.setRefundAmount(asBd(refundAgg.get("amount")));
            p.setRefundCount(asLong(refundAgg.get("count")));
        }
        List<SalesLeaderResp.MemberRefund> refundMembers = new ArrayList<>();
        List<Map<String, Object>> rmRows = mapper.refundByMember(tenantId, usernames, mStart, mEnd);
        if (rmRows != null) {
            for (Map<String, Object> r : rmRows) {
                SalesLeaderResp.MemberRefund m = new SalesLeaderResp.MemberRefund();
                m.setUsername(asString(r.get("username")));
                m.setRealName(asString(r.get("real_name")));
                m.setRefundCount(asLong(r.get("refund_count")));
                m.setRefundAmount(asBd(r.get("refund_amount")));
                m.setRefundRate(asBd(r.get("refund_rate")));
                refundMembers.add(m);
            }
        }
        p.setRefundByMember(refundMembers);

        // A7 三段漏斗（默认本月，按 lead.create_time 统计）
        p.setFunnel(buildFunnelByLeadTime(tenantId, usernames, mStart, mEnd));

        // A9 签单客户数（today / week / month）
        LocalDate today_ = LocalDate.now();
        LocalDateTime weekStart_ = today_.with(java.time.DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd_ = today_.with(java.time.DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay();
        SalesLeaderResp.SignCustomerStat sc = new SalesLeaderResp.SignCustomerStat();
        sc.setTodayCount(nvl(mapper.countSignCustomersByPeriod(tenantId, usernames, dayStart, dayEnd)));
        sc.setWeekCount(nvl(mapper.countSignCustomersByPeriod(tenantId, usernames, weekStart_, weekEnd_)));
        sc.setMonthCount(nvl(mapper.countSignCustomersByPeriod(tenantId, usernames, mStart, mEnd)));
        List<SalesLeaderResp.MemberSignCount> scMembers = new ArrayList<>();
        List<Map<String, Object>> scRows = mapper.signCustomersByMember(tenantId, usernames, dayStart, dayEnd, weekStart_, weekEnd_, mStart, mEnd);
        if (scRows != null) for (Map<String, Object> r : scRows) {
            SalesLeaderResp.MemberSignCount m = new SalesLeaderResp.MemberSignCount();
            m.setUsername(asString(r.get("username")));
            m.setRealName(asString(r.get("real_name")));
            m.setToday(asLong(r.get("today")));
            m.setWeek(asLong(r.get("week")));
            m.setMonth(asLong(r.get("month")));
            scMembers.add(m);
        }
        sc.setByMember(scMembers);
        p.setSignCustomers(sc);

        // A8 跟进频次
        List<SalesLeaderResp.HistogramBucket> hist = new ArrayList<>();
        List<Map<String, Object>> hRows = mapper.followFrequencyHistogram(tenantId, usernames);
        if (hRows != null) {
            for (Map<String, Object> r : hRows) {
                SalesLeaderResp.HistogramBucket b = new SalesLeaderResp.HistogramBucket();
                b.setLabel(asString(r.get("label")));
                b.setCount(asLong(r.get("count")));
                hist.add(b);
            }
        }
        p.setFollowFrequency(hist);

        return p;
    }

    private SalesLeaderResp.TrendBlock buildTrend(Long tenantId, List<String> usernames,
                                                   LocalDate monthStart, LocalDate prevMonthStart, LocalDate yoyMonthStart,
                                                   LocalDateTime mStart, LocalDateTime mEnd) {
        SalesLeaderResp.TrendBlock t = new SalesLeaderResp.TrendBlock();

        // B1 业绩排行（全员，前端首屏显示 TOP5，弹窗显示全部）
        List<SalesLeaderResp.PerformanceRank> ranks = new ArrayList<>();
        List<Map<String, Object>> rRows = mapper.performanceRanking(tenantId, usernames, mStart, mEnd, 200);
        int idx = 0;
        if (rRows != null) {
            for (Map<String, Object> r : rRows) {
                SalesLeaderResp.PerformanceRank pr = new SalesLeaderResp.PerformanceRank();
                pr.setRank(++idx);
                pr.setUsername(asString(r.get("username")));
                pr.setRealName(asString(r.get("real_name")));
                pr.setOrderCount(asLong(r.get("order_count")));
                pr.setOrderAmount(asBd(r.get("order_amount")));
                ranks.add(pr);
            }
        }
        t.setPerformanceRanking(ranks);

        // B2 6 月业绩走势：团队总（含金额 + 签单数）+ 本月 vs 上月增幅 TOP5 / 跌幅 TOP5
        LocalDateTime sixStart = monthStart.minusMonths(5).atStartOfDay();
        List<SalesLeaderResp.MonthlySeries> series = new ArrayList<>();
        // 团队总
        SalesLeaderResp.MonthlySeries teamSeries = new SalesLeaderResp.MonthlySeries();
        teamSeries.setName("团队总");
        teamSeries.setType("TEAM");
        teamSeries.setPoints(toMonthValuesWithCount(mapper.teamMonthlyAmount(tenantId, usernames, sixStart, mEnd)));
        series.add(teamSeries);
        // 按成员（用于绘制成员折线 + 计算本月 vs 上月增幅）
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        Map<String, String> nameMap = new HashMap<>();
        List<Map<String, Object>> mmRows = mapper.memberMonthlyAmount(tenantId, usernames, sixStart, mEnd);
        if (mmRows != null) {
            for (Map<String, Object> r : mmRows) {
                String un = asString(r.get("username"));
                grouped.computeIfAbsent(un, k -> new ArrayList<>()).add(r);
                if (!nameMap.containsKey(un)) nameMap.put(un, asString(r.get("real_name")));
            }
        }
        // 本月 vs 上月：分 3 组——上月签单≥10 单的真实涨幅/跌幅 / 上月=0 本月>0 的新晋
        // 入榜规则：
        //   涨幅榜：prevCount >= 10 && currCount > 0 && curr > prev
        //   跌幅榜：prevCount >= 10 && currCount > 0 && curr < prev   （本月 0 单不入榜）
        //   新晋  ：prevCount == 0 && currCount > 0
        // 本月签单数 = 0 一律不入涨跌榜（无业绩谈不上"幅"）；上月签单 1~9 单的低基数成员也不入
        String currMonth = monthStart.toString().substring(0, 7);
        String prevMonth = monthStart.minusMonths(1).toString().substring(0, 7);
        List<MemberDelta> realDeltas = new ArrayList<>();
        List<MemberDelta> newcomers = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, Object>>> e : grouped.entrySet()) {
            BigDecimal curr = BigDecimal.ZERO;
            BigDecimal prev = BigDecimal.ZERO;
            long prevCount = 0L;
            long currCount = 0L;
            for (Map<String, Object> r : e.getValue()) {
                String month = asString(r.get("month"));
                if (currMonth.equals(month)) {
                    curr = asBd(r.get("amount"));
                    currCount = asLong(r.get("order_count"));
                } else if (prevMonth.equals(month)) {
                    prev = asBd(r.get("amount"));
                    prevCount = asLong(r.get("order_count"));
                }
            }
            if (prev.signum() == 0 && currCount > 0) {
                // 新晋：用本月金额排序，不算百分比
                newcomers.add(new MemberDelta(e.getKey(), nameMap.get(e.getKey()), curr));
            } else if (prev.signum() > 0 && prevCount >= DELTA_PREV_MIN_COUNT && currCount > 0) {
                // 涨跌榜：上月≥10 单 + 本月有单
                BigDecimal delta = curr.subtract(prev).multiply(BigDecimal.valueOf(100))
                        .divide(prev, 2, RoundingMode.HALF_UP);
                realDeltas.add(new MemberDelta(e.getKey(), nameMap.get(e.getKey()), delta));
            }
        }
        realDeltas.sort((a, b) -> b.delta.compareTo(a.delta));
        newcomers.sort((a, b) -> b.delta.compareTo(a.delta));
        List<MemberDelta> ups = realDeltas.stream().filter(d -> d.delta.signum() > 0).limit(TOP_LIMIT).collect(Collectors.toList());
        List<MemberDelta> downs = realDeltas.stream().filter(d -> d.delta.signum() < 0)
                .sorted(Comparator.comparing(d -> d.delta)).limit(TOP_LIMIT).collect(Collectors.toList());
        List<MemberDelta> news = newcomers.stream().limit(TOP_LIMIT).collect(Collectors.toList());

        // 把后端预计算的 delta 直接放到 trend，前端不再用 series 末两点推算
        java.util.function.Function<MemberDelta, SalesLeaderResp.TopDeltaItem> toItem = md -> {
            SalesLeaderResp.TopDeltaItem it = new SalesLeaderResp.TopDeltaItem();
            it.setUsername(md.username);
            it.setRealName(md.realName);
            it.setDelta(md.delta);
            return it;
        };
        t.setUpMembers(ups.stream().map(toItem).collect(Collectors.toList()));
        t.setDownMembers(downs.stream().map(toItem).collect(Collectors.toList()));
        // 新晋：delta 字段实际存的是本月金额（MemberDelta 复用），单独映射到 currAmount
        t.setNewMembers(news.stream().map(md -> {
            SalesLeaderResp.TopDeltaItem it = new SalesLeaderResp.TopDeltaItem();
            it.setUsername(md.username);
            it.setRealName(md.realName);
            it.setCurrAmount(md.delta);
            return it;
        }).collect(Collectors.toList()));
        for (MemberDelta md : ups) {
            SalesLeaderResp.MonthlySeries s = new SalesLeaderResp.MonthlySeries();
            s.setName(md.realName != null ? md.realName : md.username);
            s.setType("UP");
            s.setPoints(toMonthValuesWithCount(grouped.get(md.username)));
            series.add(s);
        }
        for (MemberDelta md : news) {
            SalesLeaderResp.MonthlySeries s = new SalesLeaderResp.MonthlySeries();
            s.setName(md.realName != null ? md.realName : md.username);
            s.setType("NEW");
            s.setPoints(toMonthValuesWithCount(grouped.get(md.username)));
            series.add(s);
        }
        for (MemberDelta md : downs) {
            SalesLeaderResp.MonthlySeries s = new SalesLeaderResp.MonthlySeries();
            s.setName(md.realName != null ? md.realName : md.username);
            s.setType("DOWN");
            s.setPoints(toMonthValuesWithCount(grouped.get(md.username)));
            series.add(s);
        }
        t.setMonthlyAmountSeries(series);

        // B3 掉队/进步榜（本月 vs 上月，门槛 ±30%；上月签单 < 阈值的成员排除以避免低基数噪声）
        LocalDateTime prevStart = prevMonthStart.atStartOfDay();
        LocalDateTime prevEnd = monthStart.atStartOfDay();
        List<Map<String, Object>> compareRows = mapper.memberAmountCompare(tenantId, usernames, mStart, mEnd, prevStart, prevEnd);
        List<SalesLeaderResp.MemberDelta> dropList = new ArrayList<>();
        List<SalesLeaderResp.MemberDelta> upList = new ArrayList<>();
        if (compareRows != null) {
            for (Map<String, Object> r : compareRows) {
                BigDecimal curr = asBd(r.get("curr_amount"));
                BigDecimal prev = asBd(r.get("prev_amount"));
                long currCount = asLong(r.get("curr_count"));
                long prevCount = asLong(r.get("prev_count"));
                // 上月签单 < 阈值 / 上月无业绩 / 本月 0 单 一律不入榜（无业绩谈不上"幅"）
                if (prevCount < DELTA_PREV_MIN_COUNT || prev.signum() <= 0 || currCount <= 0) continue;
                BigDecimal pct = curr.subtract(prev).multiply(BigDecimal.valueOf(100))
                        .divide(prev, 2, RoundingMode.HALF_UP);
                SalesLeaderResp.MemberDelta md = new SalesLeaderResp.MemberDelta();
                md.setUsername(asString(r.get("username")));
                md.setRealName(asString(r.get("real_name")));
                md.setCurrentValue(curr);
                md.setPreviousValue(prev);
                md.setCurrentCount(currCount);
                md.setPreviousCount(prevCount);
                md.setChangePct(pct);
                if (pct.compareTo(BigDecimal.valueOf(-30)) <= 0) dropList.add(md);
                else if (pct.compareTo(BigDecimal.valueOf(30)) >= 0) upList.add(md);
            }
        }
        dropList.sort(Comparator.comparing(SalesLeaderResp.MemberDelta::getChangePct));
        upList.sort((a, b) -> b.getChangePct().compareTo(a.getChangePct()));
        t.setDropdownMembers(dropList);
        t.setUprisingMembers(upList);

        // B4 团队漏斗
        Map<String, Object> fRow = mapper.teamFunnel(tenantId, usernames, mStart, mEnd);
        SalesLeaderResp.FunnelData funnel = new SalesLeaderResp.FunnelData();
        if (fRow != null) {
            long leads = asLong(fRow.get("leads"));
            long customers = asLong(fRow.get("customers"));
            long deals = asLong(fRow.get("deals"));
            funnel.setLeads(leads);
            funnel.setCustomers(customers);
            funnel.setDeals(deals);
            funnel.setLeadToCustomer(leads > 0 ? BigDecimal.valueOf(customers * 100).divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            funnel.setCustomerToDeal(customers > 0 ? BigDecimal.valueOf(deals * 100).divide(BigDecimal.valueOf(customers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            funnel.setOverall(leads > 0 ? BigDecimal.valueOf(deals * 100).divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        }
        t.setFunnel(funnel);

        // B5 业绩同环比（金额 + 签单数）
        BigDecimal currAmount = nvlBd(mapper.teamSignAmount(tenantId, usernames, mStart, mEnd));
        BigDecimal prevAmount = nvlBd(mapper.teamSignAmount(tenantId, usernames, prevStart, prevEnd));
        BigDecimal yoyAmount = nvlBd(mapper.teamSignAmount(tenantId, usernames,
                yoyMonthStart.atStartOfDay(), yoyMonthStart.plusMonths(1).atStartOfDay()));
        t.setMonthAmount(currAmount);
        t.setMonthAmountGrowth(growthPct(currAmount, prevAmount));
        t.setMonthAmountYoyGrowth(growthPct(currAmount, yoyAmount));

        long currCount = nvl(mapper.teamSignOrderCount(tenantId, usernames, mStart, mEnd));
        long prevCount = nvl(mapper.teamSignOrderCount(tenantId, usernames, prevStart, prevEnd));
        long yoyCount = nvl(mapper.teamSignOrderCount(tenantId, usernames,
                yoyMonthStart.atStartOfDay(), yoyMonthStart.plusMonths(1).atStartOfDay()));
        t.setMonthOrderCount(currCount);
        t.setMonthOrderCountGrowth(growthPct(BigDecimal.valueOf(currCount), BigDecimal.valueOf(prevCount)));
        t.setMonthOrderCountYoyGrowth(growthPct(BigDecimal.valueOf(currCount), BigDecimal.valueOf(yoyCount)));

        // B6 客单价
        Map<String, Object> avgRow = mapper.teamAvgOrder(tenantId, usernames, mStart, mEnd);
        Map<String, Object> avgPrev = mapper.teamAvgOrder(tenantId, usernames, prevStart, prevEnd);
        Map<String, Object> avgYoy = mapper.teamAvgOrder(tenantId, usernames,
                yoyMonthStart.atStartOfDay(), yoyMonthStart.plusMonths(1).atStartOfDay());
        BigDecimal currAvg = avgRow == null ? BigDecimal.ZERO : asBd(avgRow.get("avg_amount"));
        BigDecimal prevAvg = avgPrev == null ? BigDecimal.ZERO : asBd(avgPrev.get("avg_amount"));
        BigDecimal yoyAvg = avgYoy == null ? BigDecimal.ZERO : asBd(avgYoy.get("avg_amount"));
        t.setAvgOrderAmount(currAvg);
        t.setAvgOrderAmountGrowth(growthPct(currAvg, prevAvg));
        t.setAvgOrderAmountYoyGrowth(growthPct(currAvg, yoyAvg));
        List<SalesLeaderResp.MemberAvgOrder> avgMembers = new ArrayList<>();
        List<Map<String, Object>> amRows = mapper.memberAvgOrder(tenantId, usernames, mStart, mEnd);
        if (amRows != null) {
            for (Map<String, Object> r : amRows) {
                SalesLeaderResp.MemberAvgOrder m = new SalesLeaderResp.MemberAvgOrder();
                m.setUsername(asString(r.get("username")));
                m.setRealName(asString(r.get("real_name")));
                m.setAvg(asBd(r.get("avg_amount")));
                m.setOrderCount(asLong(r.get("order_count")));
                avgMembers.add(m);
            }
        }
        t.setAvgOrderByMember(avgMembers);

        // B7 业绩构成
        List<SalesLeaderResp.PieSlice> pies = new ArrayList<>();
        List<Map<String, Object>> piRows = mapper.performanceCompositionByBusiness(tenantId, usernames, mStart, mEnd);
        if (piRows != null) {
            for (Map<String, Object> r : piRows) {
                SalesLeaderResp.PieSlice s = new SalesLeaderResp.PieSlice();
                s.setName(asString(r.get("name")));
                s.setValue(asBd(r.get("value")));
                s.setCount(asLong(r.get("count")));
                pies.add(s);
            }
        }
        t.setPerformanceComposition(pies);

        // B8 来源转化率
        List<SalesLeaderResp.SourceConversion> srcList = new ArrayList<>();
        List<Map<String, Object>> sRows = mapper.sourceConversion(tenantId, usernames, mStart, mEnd);
        Map<String, String> topMap = new HashMap<>();
        List<Map<String, Object>> topRows = mapper.sourceTopConverter(tenantId, usernames, mStart, mEnd);
        if (topRows != null) for (Map<String, Object> r : topRows) topMap.put(asString(r.get("source")), asString(r.get("top_member_name")));
        if (sRows != null) {
            for (Map<String, Object> r : sRows) {
                SalesLeaderResp.SourceConversion sc = new SalesLeaderResp.SourceConversion();
                String source = asString(r.get("source"));
                sc.setSource(source);
                sc.setLeads(asLong(r.get("leads")));
                sc.setConverted(asLong(r.get("converted")));
                sc.setRate(asBd(r.get("rate")));
                sc.setTopMemberName(topMap.get(source));
                srcList.add(sc);
            }
        }
        t.setSourceConversion(srcList);

        return t;
    }

    /** 员工诊断画像 */
    public SalesLeaderResp.EmployeeDiagnosis diagnose(LoginUser user, String username) {
        if (username == null) return null;
        Long tenantId = user.getTenantId();
        SalesLeaderResp.EmployeeDiagnosis d = new SalesLeaderResp.EmployeeDiagnosis();
        d.setUsername(username);

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime mStart = monthStart.atStartOfDay();
        LocalDateTime mEnd = monthStart.plusMonths(1).atStartOfDay();
        LocalDateTime sixStart = monthStart.minusMonths(5).atStartOfDay();

        d.setAmount6m(toMonthValues(mapper.memberMonthlyAmountSelf(tenantId, username, sixStart, mEnd)));

        Map<String, Object> fRow = mapper.memberFunnel(tenantId, username, mStart, mEnd);
        SalesLeaderResp.FunnelData funnel = new SalesLeaderResp.FunnelData();
        if (fRow != null) {
            long leads = asLong(fRow.get("leads"));
            long customers = asLong(fRow.get("customers"));
            long deals = asLong(fRow.get("deals"));
            funnel.setLeads(leads); funnel.setCustomers(customers); funnel.setDeals(deals);
            funnel.setLeadToCustomer(leads > 0 ? BigDecimal.valueOf(customers * 100).divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            funnel.setCustomerToDeal(customers > 0 ? BigDecimal.valueOf(deals * 100).divide(BigDecimal.valueOf(customers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            funnel.setOverall(leads > 0 ? BigDecimal.valueOf(deals * 100).divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        }
        d.setFunnel(funnel);

        // 近 7 天每日动作
        List<SalesLeaderResp.HistogramBucket> daily = new ArrayList<>();
        List<Map<String, Object>> dRows = mapper.memberDailyActionSelf(tenantId, username, today.minusDays(7).atStartOfDay());
        if (dRows != null) {
            for (Map<String, Object> r : dRows) {
                SalesLeaderResp.HistogramBucket b = new SalesLeaderResp.HistogramBucket();
                b.setLabel(asString(r.get("day")));
                b.setCount(asLong(r.get("count")));
                daily.add(b);
            }
        }
        d.setDailyAction7d(daily);

        // 即将回收 / 沉睡
        int leadDays = bizConfigService.getGroupInt("lead.recycle", "no_follow_days", 14);
        int custDays = bizConfigService.getGroupInt("customer.recycle", "no_follow_days", 30);
        Map<String, Object> rsRow = mapper.memberRecycleStaleSelf(tenantId, username,
                Math.max(leadDays - WARN_DAYS, 0), leadDays, Math.max(custDays - WARN_DAYS, 0), custDays,
                resolveSalesDeptUsernames());
        if (rsRow != null) {
            d.setAboutRecycleCount(asLong(rsRow.get("about_recycle_count")));
            d.setStaleCount(asLong(rsRow.get("stale_count")));
        }
        return d;
    }

    // ========== 工具 ==========
    private static class MemberDelta {
        String username, realName;
        BigDecimal delta;
        MemberDelta(String u, String r, BigDecimal d) { username = u; realName = r; delta = d; }
    }
    /**
     * A1/A2 弹窗明细列表懒加载
     * type:
     *   todayPending → { leadList, customerList, abnormalList }
     *   aboutRecycle → { leadList, customerList }
     */
    public Map<String, Object> detailList(LoginUser user, String type) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (user == null || user.getTenantId() == null || type == null) return result;
        Long tenantId = user.getTenantId();
        List<String> usernames = resolveScopeUsernames(user);

        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        if ("todayPending".equalsIgnoreCase(type)) {
            result.put("leadList", toEntityRecords(mapper.listTodayPendingLeads(tenantId, usernames, dayStart, dayEnd), null));
            result.put("customerList", toEntityRecords(mapper.listTodayPendingCustomers(tenantId, usernames, dayStart, dayEnd), null));
            result.put("abnormalList", toEntityRecords(mapper.listAbnormalRecords(tenantId, usernames), null));
        } else if ("aboutRecycle".equalsIgnoreCase(type)) {
            int leadDays = bizConfigService.getGroupInt("lead.recycle", "no_follow_days", 14);
            int custDays = bizConfigService.getGroupInt("customer.recycle", "no_follow_days", 30);
            int leadFrom = Math.max(leadDays - WARN_DAYS, 0);
            int custFrom = Math.max(custDays - WARN_DAYS, 0);
            List<String> salesDeptUsernames = resolveSalesDeptUsernames();
            result.put("leadList", toEntityRecords(mapper.listAboutRecycleLeads(tenantId, usernames, leadFrom, leadDays, leadDays, salesDeptUsernames), null));
            result.put("customerList", toEntityRecords(mapper.listAboutRecycleCustomers(tenantId, usernames, custFrom, custDays, custDays), null));
        }
        return result;
    }

    /** 漏斗按成员明细（同一时间窗）：每人 leads/customers/deals + 三档转化率 */
    public List<Map<String, Object>> funnelByMember(LoginUser user, LocalDateTime start, LocalDateTime end) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (user == null || user.getTenantId() == null) return out;
        List<String> usernames = resolveScopeUsernames(user);
        List<Map<String, Object>> rows = mapper.teamFunnelByMember(user.getTenantId(), usernames, start, end);
        if (rows == null) return out;
        for (Map<String, Object> r : rows) {
            long leads = asLong(r.get("leads"));
            long customers = asLong(r.get("customers"));
            long deals = asLong(r.get("deals"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", asString(r.get("username")));
            m.put("realName", asString(r.get("real_name")));
            m.put("leads", leads);
            m.put("customers", customers);
            m.put("deals", deals);
            m.put("leadToCustomer", leads > 0
                    ? BigDecimal.valueOf(customers * 100).divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            m.put("customerToDeal", customers > 0
                    ? BigDecimal.valueOf(deals * 100).divide(BigDecimal.valueOf(customers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            m.put("overall", leads > 0
                    ? BigDecimal.valueOf(deals * 100).divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            out.add(m);
        }
        return out;
    }

    /** 公开接口：按指定时间窗算漏斗（前端独立时间筛选用） */
    public SalesLeaderResp.FunnelData funnelByPeriod(LoginUser user, LocalDateTime start, LocalDateTime end) {
        if (user == null || user.getTenantId() == null) return new SalesLeaderResp.FunnelData();
        return buildFunnelByLeadTime(user.getTenantId(), resolveScopeUsernames(user), start, end);
    }

    /** 团队/数据范围解析：优先按"登录账号所在部门"展开，否则兜底 leader_username 范围 */
    private List<String> resolveScopeUsernames(LoginUser user) {
        List<String> deptUsernames = resolveMyDeptUsernames(user);
        if (!deptUsernames.isEmpty()) return deptUsernames;
        AnalyticsScopeResolver.ScopeResult scope = scopeResolver.resolveTeamUsers(user);
        return (scope.getUserNameList() != null && !scope.getUserNameList().isEmpty())
                ? scope.getUserNameList()
                : Collections.singletonList(user.getUsername());
    }

    private SalesLeaderResp.FunnelData buildFunnelByLeadTime(Long tenantId, List<String> usernames,
                                                              LocalDateTime start, LocalDateTime end) {
        SalesLeaderResp.FunnelData f = new SalesLeaderResp.FunnelData();
        Map<String, Object> row = mapper.teamFunnelByLeadTime(tenantId, usernames, start, end);
        if (row != null) {
            long leads = asLong(row.get("leads"));
            long customers = asLong(row.get("customers"));
            long deals = asLong(row.get("deals"));
            f.setLeads(leads); f.setCustomers(customers); f.setDeals(deals);
            f.setLeadToCustomer(leads > 0 ? BigDecimal.valueOf(customers * 100).divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            f.setCustomerToDeal(customers > 0 ? BigDecimal.valueOf(deals * 100).divide(BigDecimal.valueOf(customers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
            f.setOverall(leads > 0 ? BigDecimal.valueOf(deals * 100).divide(BigDecimal.valueOf(leads), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        }
        return f;
    }

    /** 把 mapper Map 转换为 EntityRecord */
    private List<SalesLeaderResp.EntityRecord> toEntityRecords(List<Map<String, Object>> rows, Integer threshold) {
        List<SalesLeaderResp.EntityRecord> list = new ArrayList<>();
        if (rows == null) return list;
        for (Map<String, Object> r : rows) {
            SalesLeaderResp.EntityRecord e = new SalesLeaderResp.EntityRecord();
            e.setId(asLong(r.get("id")));
            e.setName(asString(r.get("name")));
            e.setContactName(asString(r.get("contact_name")));
            e.setPhone(asString(r.get("phone")));
            e.setOwnerUser(asString(r.get("owner_user")));
            e.setOwnerUserName(asString(r.get("owner_user_name")));
            e.setLastFollowTime(asDate(r.get("last_follow_time")));
            e.setNextFollowTime(asDate(r.get("next_follow_time")));
            e.setCreateTime(asDate(r.get("create_time")));
            e.setEntityType(asString(r.get("entity_type")));
            e.setAbnormalType(asString(r.get("abnormal_type")));
            Object remain = r.get("remain_days");
            if (remain != null) e.setRemainDays(((Number) remain).intValue());
            list.add(e);
        }
        return list;
    }
    private static java.util.Date asDate(Object o) {
        if (o == null) return null;
        if (o instanceof java.util.Date) return (java.util.Date) o;
        if (o instanceof java.time.LocalDateTime) return java.util.Date.from(
                ((java.time.LocalDateTime) o).atZone(java.time.ZoneId.systemDefault()).toInstant());
        return null;
    }

    private List<SalesLeaderResp.MemberStat> toMemberStats(List<Map<String, Object>> rows) {
        List<SalesLeaderResp.MemberStat> list = new ArrayList<>();
        if (rows == null) return list;
        int rank = 0;
        for (Map<String, Object> r : rows) {
            SalesLeaderResp.MemberStat s = new SalesLeaderResp.MemberStat();
            s.setUsername(asString(r.get("username")));
            s.setRealName(asString(r.get("real_name")));
            s.setCount(asLong(r.get("count")));
            s.setRank(++rank);
            list.add(s);
        }
        return list;
    }
    private List<SalesLeaderResp.MonthValue> toMonthValues(List<Map<String, Object>> rows) {
        List<SalesLeaderResp.MonthValue> list = new ArrayList<>();
        if (rows == null) return list;
        for (Map<String, Object> r : rows) {
            SalesLeaderResp.MonthValue v = new SalesLeaderResp.MonthValue();
            v.setMonth(asString(r.get("month")));
            v.setValue(asBd(r.get("amount")));
            list.add(v);
        }
        return list;
    }
    private List<SalesLeaderResp.MonthValue> toMonthValuesWithCount(List<Map<String, Object>> rows) {
        List<SalesLeaderResp.MonthValue> list = new ArrayList<>();
        if (rows == null) return list;
        for (Map<String, Object> r : rows) {
            SalesLeaderResp.MonthValue v = new SalesLeaderResp.MonthValue();
            v.setMonth(asString(r.get("month")));
            v.setValue(asBd(r.get("amount")));
            v.setCount(asLong(r.get("order_count")));
            list.add(v);
        }
        return list;
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
