package com.qykj.finmaster.workbench.service;

import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.CustomerInfoManager;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.department.entity.Department;
import com.qykj.finmaster.system.department.manager.DepartmentManager;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import com.qykj.finmaster.workbench.dto.CsWorkOrderOverviewResp;
import com.qykj.finmaster.workbench.mapper.CsWorkOrderOverviewMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 客服部工单总览（只读统计）。工单 74 / CSWO-01。
 * 范围固定客服部（sys_department code='KFB'），不随登录人部门变化；数据源仅 work_order。
 */
@Service
public class CsWorkOrderOverviewService {

    /** 客服部部门编码 */
    private static final String CS_DEPT_CODE = "KFB";
    /** 客服部部门ID兜底（按需求 id=20，参照 LeadRecycleConstants.SALES_DEPT_ID 范式） */
    private static final long CS_DEPT_ID_FALLBACK = 20L;
    /** 停滞阈值（天）：在手且 update_time 超过该天数未更新 */
    private static final int STALE_DAYS = 3;
    /** 超时明细 Top 上限 */
    private static final int OVERDUE_LIMIT = 50;

    @Autowired
    private CsWorkOrderOverviewMapper mapper;
    @Autowired
    private DepartmentManager departmentManager;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CustomerCompanyManager customerCompanyManager;
    @Autowired
    private CustomerInfoManager customerInfoManager;

    public CsWorkOrderOverviewResp overview(LoginUser user, String taskType, String month) {
        CsWorkOrderOverviewResp resp = new CsWorkOrderOverviewResp();
        Date now = new Date();
        resp.setGeneratedAt(now);
        if (user == null || user.getTenantId() == null) {
            return resp;
        }
        Long tenantId = user.getTenantId();

        // 1) 解析客服部（code=KFB），取 deptId 与部门名
        Long deptId = CS_DEPT_ID_FALLBACK;
        String deptName = "客服部";
        Department dept = departmentManager.lambdaQuery()
                .eq(Department::getCode, CS_DEPT_CODE)
                .eq(Department::getTenantId, tenantId)
                .last("limit 1")
                .one();
        if (dept != null) {
            deptId = dept.getId();
            if (dept.getName() != null) deptName = dept.getName();
        }
        resp.setDeptName(deptName);

        // 2) 客服部全量成员名单（含 0 工单成员）
        List<User> members = userMapper.selectUsersByDeptWithChildren(deptId, null);
        LinkedHashMap<String, Map<String, Object>> seed = new LinkedHashMap<>();
        Map<String, String> ownerNameMap = new HashMap<>();
        if (members != null) {
            for (User u : members) {
                if (u == null || u.getUsername() == null) continue;
                seed.put(u.getUsername(), zeroMember(u.getUsername(), u.getRealName()));
                if (u.getRealName() != null && !u.getRealName().isEmpty()) ownerNameMap.put(u.getUsername(), u.getRealName());
            }
        }
        if (seed.isEmpty()) {
            resp.setMembers(new ArrayList<>());
            resp.setKpi(emptyKpi());
            resp.setOverduePendingTop(new ArrayList<>());
            resp.setOverdueRunningTop(new ArrayList<>());
            resp.setSuspendReasons(new ArrayList<>());
            resp.setSuspendDetail(new ArrayList<>());
            return resp;
        }
        List<String> usernames = new ArrayList<>(seed.keySet());

        // 3) 当月区间
        LocalDate today = LocalDate.now();
        Date monthStart = toDate(today.withDayOfMonth(1));
        Date monthEnd = toDate(today.withDayOfMonth(1).plusMonths(1));
        // CSWO-05：按创建月份过滤窗口（yyyy-MM），空白则 null（结果与现状一致）
        Date[] cw = parseCreateWindow(month);
        Date createStart = cw[0], createEnd = cw[1];

        // 4) 按人统计 overlay 到种子（无数据保持 0）
        List<Map<String, Object>> rows = mapper.overviewByMember(tenantId, usernames, blankToNull(taskType), now, monthStart, monthEnd, createStart, createEnd, STALE_DAYS);
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String un = asString(r.get("username"));
                if (un == null) continue;
                Map<String, Object> m = seed.get(un);
                // 口径：current_owner_user 已限定客服部成员；非种子成员（理论上不会出现）直接跳过，不新建非客服行
                if (m == null) continue;
                if (m.get("realName") == null && r.get("real_name") != null) m.put("realName", asString(r.get("real_name")));
                m.put("pending", asLong(r.get("pending")));
                m.put("running", asLong(r.get("running")));
                m.put("waitingChild", asLong(r.get("waiting_child")));
                m.put("suspended", asLong(r.get("suspended")));
                m.put("concessionDone", asLong(r.get("concession_done")));
                m.put("waitingFix", asLong(r.get("waiting_fix")));
                m.put("activeCount", asLong(r.get("active_count")));
                m.put("monthDone", asLong(r.get("month_done")));
                m.put("overduePending", asLong(r.get("overdue_pending")));
                m.put("overdueRunning", asLong(r.get("overdue_running")));
                m.put("staleCount", asLong(r.get("stale_count")));
            }
        }
        List<Map<String, Object>> memberList = new ArrayList<>(seed.values());
        // 默认按在手合计倒序，其次超时多者靠前
        memberList.sort((a, b) -> {
            int c = Long.compare(asLong(b.get("activeCount")), asLong(a.get("activeCount")));
            if (c != 0) return c;
            long ob = asLong(b.get("overduePending")) + asLong(b.get("overdueRunning"));
            long oa = asLong(a.get("overduePending")) + asLong(a.get("overdueRunning"));
            return Long.compare(ob, oa);
        });
        resp.setMembers(memberList);

        // 5) KPI 汇总
        resp.setKpi(buildKpi(memberList));

        // 6) 超时明细（待处理 / 处理中）
        resp.setOverduePendingTop(normalizeOverdue(mapper.overdueDetail(tenantId, usernames, blankToNull(taskType), "PENDING", now, createStart, createEnd, OVERDUE_LIMIT), ownerNameMap));
        resp.setOverdueRunningTop(normalizeOverdue(mapper.overdueDetail(tenantId, usernames, blankToNull(taskType), "RUNNING", now, createStart, createEnd, OVERDUE_LIMIT), ownerNameMap));

        // 7) 挂起原因汇总 + 明细
        resp.setSuspendReasons(normalizeSuspendReasons(mapper.suspendReasonSummary(tenantId, usernames, blankToNull(taskType), createStart, createEnd)));
        resp.setSuspendDetail(normalizeSuspendDetail(mapper.suspendDetail(tenantId, usernames, blankToNull(taskType), now, createStart, createEnd), ownerNameMap));

        return resp;
    }

    /**
     * 下钻明细（工单 74 / CSWO-03）：按 bucket 返回客服部持有工单分页列表，补公司名/客户名。
     * @param username 非空则只看该人；空=整客服部
     * @param bucket   active/pending/running/waitingChild/suspended/concessionDone/waitingFix/
     *                 overduePending/overdueRunning/monthDone/stale
     */
    public Map<String, Object> drill(LoginUser user, String bucket, String username, String taskType,
                                     String month, Integer pageNum, Integer pageSize) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("rows", new ArrayList<>());
        resp.put("total", 0L);
        if (user == null || user.getTenantId() == null) return resp;
        Long tenantId = user.getTenantId();

        List<User> members = loadCsMembers(tenantId);
        List<String> usernames = new ArrayList<>();
        Map<String, String> ownerNameMap = new HashMap<>();
        for (User u : members) {
            if (u == null || u.getUsername() == null) continue;
            usernames.add(u.getUsername());
            if (u.getRealName() != null && !u.getRealName().isEmpty()) ownerNameMap.put(u.getUsername(), u.getRealName());
        }
        if (usernames.isEmpty()) return resp;

        Date now = new Date();
        LocalDate today = LocalDate.now();
        Date monthStart = toDate(today.withDayOfMonth(1));
        Date monthEnd = toDate(today.withDayOfMonth(1).plusMonths(1));
        // CSWO-05：按创建月份过滤窗口（yyyy-MM），空白则 null（结果与现状一致）
        Date[] cw = parseCreateWindow(month);
        Date createStart = cw[0], createEnd = cw[1];
        int pn = (pageNum == null || pageNum <= 0) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize <= 0) ? 20 : pageSize;
        int offset = (pn - 1) * ps;
        String un = blankToNull(username);
        String tt = blankToNull(taskType);
        String b = blankToNull(bucket);

        Long total = mapper.drillCount(tenantId, usernames, un, b, tt, now, monthStart, monthEnd, createStart, createEnd, STALE_DAYS);
        List<Map<String, Object>> rows = mapper.drillList(tenantId, usernames, un, b, tt, now, monthStart, monthEnd, createStart, createEnd, STALE_DAYS, offset, ps);

        // 批量反查公司名 / 客户名
        Set<Long> companyIds = new HashSet<>();
        Set<Long> customerIds = new HashSet<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                Long cid = asLongObj(r.get("company_id"));
                Long kid = asLongObj(r.get("customer_id"));
                if (cid != null) companyIds.add(cid);
                if (kid != null) customerIds.add(kid);
            }
        }
        Map<Long, String> companyNames = new HashMap<>();
        if (!companyIds.isEmpty()) {
            for (CustomerCompany c : customerCompanyManager.listByIds(companyIds)) {
                if (c != null) companyNames.put(c.getId(), c.getCompanyName());
            }
        }
        Map<Long, String> customerNames = new HashMap<>();
        if (!customerIds.isEmpty()) {
            for (CustomerInfo c : customerInfoManager.listByIds(customerIds)) {
                if (c != null) customerNames.put(c.getId(), c.getCustomerName());
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                Long cid = asLongObj(r.get("company_id"));
                Long kid = asLongObj(r.get("customer_id"));
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.get("id"));
                m.put("taskNo", r.get("task_no"));
                m.put("taskType", r.get("task_type"));
                m.put("title", r.get("title"));
                m.put("currentOwnerUser", r.get("current_owner_user"));
                m.put("currentOwnerName", resolveOwnerName(r.get("current_owner_name"), r.get("current_owner_user"), ownerNameMap));
                m.put("status", r.get("status"));
                m.put("dueTime", r.get("due_time"));
                m.put("createTime", r.get("create_time"));
                m.put("companyId", cid);
                m.put("customerId", kid);
                m.put("companyName", cid != null ? companyNames.get(cid) : null);
                m.put("customerName", kid != null ? customerNames.get(kid) : null);
                out.add(m);
            }
        }
        resp.put("rows", out);
        resp.put("total", total != null ? total : 0L);
        return resp;
    }

    /** 加载客服部（code=KFB，兜底 20）全量成员。 */
    private List<User> loadCsMembers(Long tenantId) {
        Long deptId = CS_DEPT_ID_FALLBACK;
        Department dept = departmentManager.lambdaQuery()
                .eq(Department::getCode, CS_DEPT_CODE)
                .eq(Department::getTenantId, tenantId)
                .last("limit 1")
                .one();
        if (dept != null) deptId = dept.getId();
        List<User> members = userMapper.selectUsersByDeptWithChildren(deptId, null);
        return members != null ? members : new ArrayList<>();
    }

    /** 持有人显示名兜底：current_owner_name 非空 → 它；否则成员名映射；再否则回退 username。 */
    private static String resolveOwnerName(Object currentOwnerName, Object currentOwnerUser, Map<String, String> ownerNameMap) {
        String name = asString(currentOwnerName);
        if (name != null && !name.isEmpty()) return name;
        String un = asString(currentOwnerUser);
        if (un == null) return null;
        String mapped = ownerNameMap == null ? null : ownerNameMap.get(un);
        return (mapped != null && !mapped.isEmpty()) ? mapped : un;
    }

    private static Long asLongObj(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return null; }
    }

    private Map<String, Object> zeroMember(String username, String realName) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", username);
        m.put("realName", realName != null ? realName : username);
        for (String k : new String[]{"pending", "running", "waitingChild", "suspended", "concessionDone",
                "waitingFix", "activeCount", "monthDone", "overduePending", "overdueRunning", "staleCount"}) {
            m.put(k, 0L);
        }
        return m;
    }

    private Map<String, Object> buildKpi(List<Map<String, Object>> members) {
        Map<String, Object> kpi = emptyKpi();
        long memberCount = members.size();
        long active = 0, pending = 0, running = 0, overduePending = 0, overdueRunning = 0, suspended = 0, monthDone = 0, stale = 0;
        for (Map<String, Object> m : members) {
            active += asLong(m.get("activeCount"));
            pending += asLong(m.get("pending"));
            running += asLong(m.get("running"));
            overduePending += asLong(m.get("overduePending"));
            overdueRunning += asLong(m.get("overdueRunning"));
            suspended += asLong(m.get("suspended"));
            monthDone += asLong(m.get("monthDone"));
            stale += asLong(m.get("staleCount"));
        }
        kpi.put("memberCount", memberCount);
        kpi.put("active", active);
        kpi.put("pending", pending);
        kpi.put("running", running);
        kpi.put("overduePending", overduePending);
        kpi.put("overdueRunning", overdueRunning);
        kpi.put("suspended", suspended);
        kpi.put("monthDone", monthDone);
        kpi.put("stale", stale);
        return kpi;
    }

    private Map<String, Object> emptyKpi() {
        Map<String, Object> kpi = new LinkedHashMap<>();
        for (String k : new String[]{"memberCount", "active", "pending", "running", "overduePending",
                "overdueRunning", "suspended", "monthDone", "stale"}) {
            kpi.put(k, 0L);
        }
        return kpi;
    }

    private List<Map<String, Object>> normalizeOverdue(List<Map<String, Object>> rows, Map<String, String> ownerNameMap) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) return out;
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id"));
            m.put("taskNo", r.get("task_no"));
            m.put("title", r.get("title"));
            m.put("currentOwnerUser", r.get("current_owner_user"));
            m.put("currentOwnerName", resolveOwnerName(r.get("current_owner_name"), r.get("current_owner_user"), ownerNameMap));
            m.put("dueTime", r.get("due_time"));
            m.put("overdueDays", asLong(r.get("overdue_days")));
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> normalizeSuspendReasons(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) return out;
        for (Map<String, Object> r : rows) {
            String code = asString(r.get("reason"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("reasonCode", code);
            m.put("name", suspendReasonName(code));
            m.put("value", asLong(r.get("cnt")));
            out.add(m);
        }
        return out;
    }

    private List<Map<String, Object>> normalizeSuspendDetail(List<Map<String, Object>> rows, Map<String, String> ownerNameMap) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (rows == null) return out;
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.get("id"));
            m.put("taskNo", r.get("task_no"));
            m.put("title", r.get("title"));
            m.put("currentOwnerUser", r.get("current_owner_user"));
            m.put("currentOwnerName", resolveOwnerName(r.get("current_owner_name"), r.get("current_owner_user"), ownerNameMap));
            String code = asString(r.get("suspend_reason"));
            m.put("suspendReason", code);
            m.put("suspendReasonName", suspendReasonName(code));
            m.put("suspendRemark", r.get("suspend_remark"));
            m.put("plannedResumeTime", r.get("planned_resume_time"));
            m.put("resumeOverdue", asLong(r.get("resume_overdue")) == 1L);
            out.add(m);
        }
        return out;
    }

    /** 挂起原因字典映射（CUSTOMER_ABNORMAL/NO_TAX/NO_CONTACT，见 V2.0.71） */
    private String suspendReasonName(String code) {
        if (code == null || code.isEmpty()) return "未填原因";
        switch (code) {
            case "CUSTOMER_ABNORMAL": return "客户异常";
            case "NO_TAX": return "未开税";
            case "NO_CONTACT": return "联系不上";
            default: return code;
        }
    }

    private static String blankToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 解析创建月份过滤窗口（CSWO-05）：yyyy-MM → [当月1号, 次月1号]；空白或非法 → [null, null]（不过滤、与现状一致）。
     */
    private static Date[] parseCreateWindow(String month) {
        String m = blankToNull(month);
        if (m == null) return new Date[]{null, null};
        try {
            java.time.YearMonth ym = java.time.YearMonth.parse(m);
            return new Date[]{toDate(ym.atDay(1)), toDate(ym.atDay(1).plusMonths(1))};
        } catch (Exception e) {
            return new Date[]{null, null};
        }
    }

    private static long asLong(Object o) {
        if (o == null) return 0L;
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(o.toString().trim()); } catch (Exception e) { return 0L; }
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
