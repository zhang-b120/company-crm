package com.qykj.finmaster.analytics.common.enums;

/**
 * 统计时间范围枚举。CUSTOM 时需要外部传入 start/end。
 */
public enum TimeRangeType {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    LAST_WEEK,
    THIS_MONTH,
    LAST_MONTH,
    THIS_QUARTER,
    THIS_YEAR,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM;

    public static TimeRangeType fromString(String v) {
        if (v == null || v.isEmpty()) {
            return THIS_MONTH;
        }
        try {
            return TimeRangeType.valueOf(v.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return THIS_MONTH;
        }
    }
}
