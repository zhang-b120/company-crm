package com.qykj.finmaster.workbench.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.analytics.common.enums.AnalyticsScope;
import com.qykj.finmaster.analytics.common.service.AnalyticsScopeResolver;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.role.entity.Role;
import com.qykj.finmaster.system.role.mapper.RoleMapper;
import com.qykj.finmaster.workbench.dto.WorkbenchConfigResp;
import com.qykj.finmaster.workbench.dto.WorkbenchItem;
import com.qykj.finmaster.workbench.enums.WorkbenchType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工作台配置：按用户角色 + 权限决定可见的工作台列表
 *
 * 规则（V2.0.68 起改为按角色分发，不再用 sys_department.workbench_type）：
 *   1. 超管 / admin / 含公司级权限的用户 → 4 个工作台全可见 (BOSS + MARKET + SALES + CS)
 *   2. 否则取用户所有角色的 workbench_type 并集（互斥：每个角色只能绑定一种类型）
 *   3. 角色绑定的 NONE 类型不展示；BOSS 类型出现时强制 COMPANY 视角
 *   4. 默认激活：取用户角色列表中第一个非 NONE 的工作台类型
 */
@Service
public class WorkbenchConfigService {

    @Autowired
    private AnalyticsScopeResolver scopeResolver;

    @Autowired
    private RoleMapper roleMapper;

    public WorkbenchConfigResp getConfig(LoginUser user) {
        WorkbenchConfigResp out = new WorkbenchConfigResp();
        out.setWorkbenches(new ArrayList<>());
        out.setIsCompanyScope(Boolean.FALSE);

        if (user == null || user.getUserId() == null) {
            return out;
        }

        AnalyticsScope max = scopeResolver.maxAllowedScope(user);
        boolean company = max == AnalyticsScope.COMPANY;
        out.setIsCompanyScope(company);

        Set<String> visibleTypes = new HashSet<>();
        String defaultType = null;

        // 是否团队长（角色绑定 SALES_LEADER / CS_LEADER 类型，自动开启 TEAM 默认视角）
        boolean salesLeader = false;
        boolean csLeader = false;

        if (company) {
            // 公司级用户（超管 / admin / 老板）：四个工作台全部可见
            visibleTypes.addAll(Arrays.asList(
                    WorkbenchType.BOSS.getCode(),
                    WorkbenchType.MARKET.getCode(),
                    WorkbenchType.SALES.getCode(),
                    WorkbenchType.CS.getCode()));
        } else {
            // 按角色 workbench_type 取并集；LEADER 类型映射到对应业务 Tab（SALES_LEADER → SALES）
            List<Long> roleIds = user.getRoleIds();
            if (roleIds != null && !roleIds.isEmpty()) {
                List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>()
                        .in(Role::getId, roleIds)
                        .eq(Role::getEnabled, true));
                if (roles != null) {
                    for (Role r : roles) {
                        String wt = r.getWorkbenchType();
                        if (!WorkbenchType.isBusinessType(wt)) continue;
                        String upper = wt.toUpperCase();
                        if ("SALES_LEADER".equals(upper)) {
                            salesLeader = true;
                            visibleTypes.add(WorkbenchType.SALES.getCode());
                            if (defaultType == null) defaultType = WorkbenchType.SALES.getCode();
                        } else if ("CS_LEADER".equals(upper)) {
                            csLeader = true;
                            visibleTypes.add(WorkbenchType.CS.getCode());
                            if (defaultType == null) defaultType = WorkbenchType.CS.getCode();
                        } else {
                            visibleTypes.add(upper);
                            if (defaultType == null) defaultType = upper;
                        }
                    }
                }
            }
        }
        out.setSalesLeader(salesLeader);
        out.setCsLeader(csLeader);

        // 按固定顺序输出 BOSS / MARKET / SALES / CS
        boolean foundDefault = defaultType != null;
        for (WorkbenchType t : new WorkbenchType[]{
                WorkbenchType.BOSS, WorkbenchType.MARKET, WorkbenchType.SALES, WorkbenchType.CS}) {
            if (!visibleTypes.contains(t.getCode())) continue;
            WorkbenchItem item = new WorkbenchItem();
            item.setType(t.getCode());
            item.setLabel(t.getLabel());
            // BOSS 工作台强制 COMPANY 视角，不提供切换选项
            if (t == WorkbenchType.BOSS) {
                item.setAvailableScopes(java.util.Collections.singletonList("COMPANY"));
            } else if ((t == WorkbenchType.SALES && salesLeader)
                    || (t == WorkbenchType.CS && csLeader)
                    || company) {
                // 团队长 / 公司级：可在 PERSONAL / TEAM 之间切换；默认 TEAM
                item.setAvailableScopes(Arrays.asList("PERSONAL", "TEAM"));
                item.setDefaultScope("TEAM");
            } else {
                // 普通员工只看个人
                item.setAvailableScopes(java.util.Collections.singletonList("PERSONAL"));
                item.setDefaultScope("PERSONAL");
            }
            if (!foundDefault) {
                item.setDefaultActive(Boolean.TRUE);
                foundDefault = true;
            } else {
                item.setDefaultActive(t.getCode().equals(defaultType));
            }
            out.getWorkbenches().add(item);
        }
        return out;
    }
}
