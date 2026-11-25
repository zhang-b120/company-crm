package com.qykj.finmaster.analytics.common.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.analytics.common.enums.AnalyticsScope;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.department.entity.Department;
import com.qykj.finmaster.system.department.manager.DepartmentManager;
import com.qykj.finmaster.system.role.entity.Role;
import com.qykj.finmaster.system.role.mapper.RoleMapper;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.mapper.UserDepartmentMapper;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 判定当前用户的最高可见层级，并解析对应的用户/部门白名单
 *
 * 规则（与用户确认一致，沿用 Dept.leaderUsername 作为团队长身份）：
 *   maxScope =
 *     if user.isSuperAdmin || 具有老板/管理员角色名: COMPANY
 *     else if 存在 sys_department where leader_username = user.username: TEAM
 *     else: PERSONAL
 *   effective = min(maxScope, requestedScope)
 */
@Component
public class AnalyticsScopeResolver {

    /** 视为公司级可见的角色 Key 白名单（可按实际角色表扩展） */
    private static final Set<String> COMPANY_ROLES = new HashSet<>(
            java.util.Arrays.asList("boss", "admin", "ceo", "super_admin", "general_manager"));

    @Autowired
    private DepartmentManager departmentManager;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserDepartmentMapper userDepartmentMapper;

    @Autowired
    private RoleMapper roleMapper;

    /**
     * 计算当前用户的最高可见层级（不限定工作台类型）
     */
    public AnalyticsScope maxAllowedScope(LoginUser user) {
        return maxAllowedScope(user, null);
    }

    /**
     * 计算当前用户在指定工作台下的最高可见层级
     * workbenchType 为 null 时沿用原逻辑（任意 leader 部门即 TEAM）
     * 传入 MARKET/SALES/CS 时，TEAM 权限需要用户是该类型部门的 leader
     */
    public AnalyticsScope maxAllowedScope(LoginUser user, String workbenchType) {
        if (user == null || user.getUsername() == null) {
            return AnalyticsScope.PERSONAL;
        }
        if (user.isSuperAdmin() || user.isAdmin()) {
            return AnalyticsScope.COMPANY;
        }
        // 角色判断：permissions 里如果包含 *:all 或特定角色权限标识视为 COMPANY
        if (user.getPermissions() != null) {
            for (String p : user.getPermissions()) {
                if (p == null) continue;
                String low = p.toLowerCase();
                if (low.equals("*:*:*") || low.startsWith("boss:") || low.startsWith("analytics:company")) {
                    return AnalyticsScope.COMPANY;
                }
            }
        }
        // BOSS 角色 / BOSS 部门成员自动获得公司级数据权限（业务上"老板配置的老板"视角）
        if (isBossRoleMember(user) || isBossDeptMember(user)) {
            return AnalyticsScope.COMPANY;
        }
        // 团队长判定：存在以当前用户名为 leaderUsername 的部门（若指定了工作台类型，要求同类型）
        List<Department> ledDepts = findLeaderDepartments(user, workbenchType);
        if (!ledDepts.isEmpty()) {
            return AnalyticsScope.TEAM;
        }
        return AnalyticsScope.PERSONAL;
    }

    /**
     * 判定用户是否绑定了 BOSS 工作台角色（V2.0.68 起角色驱动工作台）
     */
    public boolean isBossRoleMember(LoginUser user) {
        if (user == null || user.getRoleIds() == null || user.getRoleIds().isEmpty()) {
            return false;
        }
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<Role>()
                .in(Role::getId, user.getRoleIds())
                .eq(Role::getEnabled, true)
                .eq(Role::getWorkbenchType, "BOSS"));
        return count != null && count > 0;
    }

    /**
     * 判定用户是否属于任意绑定了 BOSS 工作台的部门（含 dept_id 主部门 + 关联多部门）
     * 兼容 V2.0.68 之前的部门级配置，新数据以角色配置为准
     */
    public boolean isBossDeptMember(LoginUser user) {
        if (user == null || user.getUserId() == null || user.getTenantId() == null) {
            return false;
        }
        Set<Long> deptIds = new HashSet<>();
        User u = userMapper.selectById(user.getUserId());
        if (u != null && u.getDeptId() != null && u.getDeptId() > 0) {
            deptIds.add(u.getDeptId());
        }
        List<Long> related = userDepartmentMapper.findDepartmentIdsByUserId(user.getUserId());
        if (related != null) {
            deptIds.addAll(related);
        }
        if (deptIds.isEmpty()) return false;
        Long count = departmentManager.lambdaQuery()
                .in(Department::getId, deptIds)
                .eq(Department::getTenantId, user.getTenantId())
                .eq(Department::getEnabled, true)
                .eq(Department::getWorkbenchType, "BOSS")
                .count();
        return count != null && count > 0;
    }

    /**
     * 当前登录人作为 leader 的所有部门（不限定工作台类型）
     */
    public List<Department> findLeaderDepartments(LoginUser user) {
        return findLeaderDepartments(user, null);
    }

    /**
     * 当前登录人作为 leader 的部门，可选按工作台类型过滤
     */
    public List<Department> findLeaderDepartments(LoginUser user, String workbenchType) {
        if (user == null || user.getUsername() == null || user.getTenantId() == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Department> q = new LambdaQueryWrapper<Department>()
                .eq(Department::getTenantId, user.getTenantId())
                .eq(Department::getLeaderUsername, user.getUsername())
                .eq(Department::getEnabled, true);
        if (workbenchType != null && !workbenchType.isEmpty()) {
            q.eq(Department::getWorkbenchType, workbenchType);
        }
        return departmentManager.list(q);
    }

    /**
     * 解析 TEAM 层级下可见的用户列表（部门及子部门展开）
     */
    public ScopeResult resolveTeamUsers(LoginUser user) {
        return resolveTeamUsers(user, null);
    }

    /**
     * 解析指定工作台类型下 TEAM 层级可见的用户列表
     */
    public ScopeResult resolveTeamUsers(LoginUser user, String workbenchType) {
        List<Department> ledDepts = findLeaderDepartments(user, workbenchType);
        if (ledDepts.isEmpty()) {
            return ScopeResult.onlySelf(user);
        }
        Set<String> userNames = new HashSet<>();
        Set<Long> userIds = new HashSet<>();
        List<Long> deptIds = new ArrayList<>();
        for (Department dept : ledDepts) {
            deptIds.add(dept.getId());
            List<User> usersInDept = userMapper.selectUsersByDeptWithChildren(dept.getId(), 1);
            if (usersInDept == null) continue;
            for (User u : usersInDept) {
                if (u == null) continue;
                if (u.getUsername() != null) userNames.add(u.getUsername());
                if (u.getId() != null) userIds.add(u.getId());
            }
        }
        // 团队长本人也要纳入
        if (user.getUsername() != null) userNames.add(user.getUsername());
        if (user.getUserId() != null) userIds.add(user.getUserId());
        return new ScopeResult(new ArrayList<>(userNames), new ArrayList<>(userIds), deptIds);
    }

    /**
     * 解析 PERSONAL 层级：仅本人
     */
    public ScopeResult resolvePersonal(LoginUser user) {
        return ScopeResult.onlySelf(user);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ScopeResult {
        private List<String> userNameList;
        private List<Long> userIdList;
        private List<Long> deptIdList;

        public static ScopeResult onlySelf(LoginUser user) {
            if (user == null || user.getUsername() == null) {
                return new ScopeResult(
                        Collections.singletonList("__nobody__"),
                        Collections.singletonList(-1L),
                        Collections.emptyList());
            }
            return new ScopeResult(
                    Collections.singletonList(user.getUsername()),
                    user.getUserId() == null
                            ? Collections.singletonList(-1L)
                            : Collections.singletonList(user.getUserId()),
                    Collections.emptyList());
        }
    }

    /**
     * 工具：将 max 和 requested 取最低层级（COMPANY > TEAM > PERSONAL）
     */
    public AnalyticsScope cap(AnalyticsScope max, AnalyticsScope requested) {
        if (max == null) max = AnalyticsScope.PERSONAL;
        if (requested == null) return max;
        return ordinalOf(requested) <= ordinalOf(max) ? requested : max;
    }

    private int ordinalOf(AnalyticsScope s) {
        switch (s) {
            case PERSONAL: return 0;
            case TEAM: return 1;
            case COMPANY: return 2;
            default: return 0;
        }
    }

    /** 供前端查询当前用户可用层级列表 */
    public List<String> availableScopes(LoginUser user) {
        return availableScopes(user, null);
    }

    /** 指定工作台下的可用层级列表 */
    public List<String> availableScopes(LoginUser user, String workbenchType) {
        AnalyticsScope max = maxAllowedScope(user, workbenchType);
        List<String> out = new ArrayList<>();
        out.add(AnalyticsScope.PERSONAL.name());
        if (max == AnalyticsScope.TEAM || max == AnalyticsScope.COMPANY) {
            out.add(AnalyticsScope.TEAM.name());
        }
        if (max == AnalyticsScope.COMPANY) {
            out.add(AnalyticsScope.COMPANY.name());
        }
        return out;
    }
}
