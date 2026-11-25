package com.qykj.finmaster.analytics.common.context;

import com.qykj.finmaster.analytics.common.enums.AnalyticsScope;
import com.qykj.finmaster.analytics.common.enums.CompareMode;
import com.qykj.finmaster.analytics.common.enums.TimeRangeType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 统计查询的运行时上下文
 * 由 AnalyticsContextBuilder 根据 LoginUser + AnalyticsQueryReq 构建
 * 传入 Mapper 参数名 "ctx"，XML 中用 ctx.scope / ctx.userNameList / ctx.timeStart 等访问
 */
@Data
public class AnalyticsContext {

    /** 实际生效的层级（已按权限 cap） */
    private AnalyticsScope scope;

    /** 可见用户名列表（PERSONAL/TEAM 使用，COMPANY 为 null 表示不下推过滤） */
    private List<String> userNameList;

    /** 可见用户ID列表（配 CallRecord/FieldWorkTask 等以 ID 为外键的表） */
    private List<Long> userIdList;

    /** 可见部门ID列表（团队长聚合到部门时使用） */
    private List<Long> deptIdList;

    /** 租户ID，super admin 可能为 null */
    private Long tenantId;

    /** 当期时间区间起点（含） */
    private LocalDateTime timeStart;

    /** 当期时间区间终点（含） */
    private LocalDateTime timeEnd;

    /** 对比期起点，compareMode=NONE 时为 null */
    private LocalDateTime prevTimeStart;

    /** 对比期终点，compareMode=NONE 时为 null */
    private LocalDateTime prevTimeEnd;

    /** 对比模式 */
    private CompareMode compareMode;

    /** 当期时间范围类型，可用于前端展示与切片策略选择 */
    private TimeRangeType timeRangeType;

    /** 当前登录用户名，部分指标（如"我的私海"）需要 */
    private String currentUsername;

    /** 当前登录用户ID */
    private Long currentUserId;

    public boolean isCompany() {
        return scope == AnalyticsScope.COMPANY;
    }

    public boolean hasCompare() {
        return compareMode != null && compareMode != CompareMode.NONE
                && prevTimeStart != null && prevTimeEnd != null;
    }

    /** 空列表安全访问 */
    public List<String> safeUserNameList() {
        return userNameList == null ? Collections.emptyList() : userNameList;
    }

    public List<Long> safeUserIdList() {
        return userIdList == null ? Collections.emptyList() : userIdList;
    }

    public List<Long> safeDeptIdList() {
        return deptIdList == null ? Collections.emptyList() : deptIdList;
    }
}
