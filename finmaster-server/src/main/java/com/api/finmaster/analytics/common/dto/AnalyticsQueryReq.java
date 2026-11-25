package com.qykj.finmaster.analytics.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统计接口通用请求体
 * - scope 不传默认 PERSONAL，Resolver 会根据用户权限 cap 到最高可见层级
 * - timeRangeType = CUSTOM 时必须传 startTime 和 endTime
 * - compareWith 不传默认 NONE
 */
@Data
public class AnalyticsQueryReq {

    /** PERSONAL / TEAM / COMPANY */
    private String scope;

    /** TODAY / THIS_WEEK / THIS_MONTH / ... / CUSTOM */
    private String timeRangeType;

    /** CUSTOM 时必填 */
    private LocalDateTime startTime;

    /** CUSTOM 时必填 */
    private LocalDateTime endTime;

    /** NONE / WOW / MOM / YOY */
    private String compareWith;

    /**
     * 工作台类型（可选）：MARKET / SALES / CS
     * 工作台页面查询时带上；Resolver 会据此只展开对应类型部门的 leader 权限
     * 传 null 则沿用"任意 leader 部门即为 TEAM"的老逻辑（数据中心使用）
     */
    private String workbenchType;
}
