package com.qykj.finmaster.analytics.common.enums;

/**
 * 数据分析可见范围层级
 * PERSONAL: 仅本人创建/负责的数据
 * TEAM:     团队长可见的部门及子部门全部用户数据
 * COMPANY:  全租户数据
 */
public enum AnalyticsScope {
    PERSONAL,
    TEAM,
    COMPANY;

    public static AnalyticsScope fromString(String v) {
        if (v == null || v.isEmpty()) {
            return PERSONAL;
        }
        try {
            return AnalyticsScope.valueOf(v.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PERSONAL;
        }
    }
}
