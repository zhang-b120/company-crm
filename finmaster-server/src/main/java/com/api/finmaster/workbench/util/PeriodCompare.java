package com.qykj.finmaster.workbench.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 同环比时段计算器
 *
 * 给定一个 [start, end) 区间，计算：
 *   prev  - 等长前一段（环比）：长度相同的紧邻前一段
 *   yoy   - 同比去年同期：起止时间整体向前推一年
 */
public final class PeriodCompare {

    private PeriodCompare() {}

    public static class Range {
        public final LocalDateTime start;
        public final LocalDateTime end;
        public Range(LocalDateTime s, LocalDateTime e) { this.start = s; this.end = e; }
    }

    /** 环比前一段：[start - len, start) */
    public static Range prev(LocalDateTime start, LocalDateTime end) {
        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) days = 1;
        return new Range(start.minusDays(days), start);
    }

    /** 同比去年同期：start.minusYears(1) ~ end.minusYears(1) */
    public static Range yoy(LocalDateTime start, LocalDateTime end) {
        return new Range(start.minusYears(1), end.minusYears(1));
    }

    /**
     * 已知周期类型时使用更语义化的环比起止：
     * - TODAY: 昨天
     * - WEEK: 上周一 ~ 本周一
     * - MONTH: 上月 1 号 ~ 本月 1 号
     * - 其他/CUSTOM: 等长前一段
     */
    public static Range prevByType(String periodType, LocalDateTime start, LocalDateTime end) {
        if (periodType == null) return prev(start, end);
        switch (periodType.toUpperCase()) {
            case "TODAY": {
                LocalDate y = LocalDate.now().minusDays(1);
                return new Range(y.atStartOfDay(), LocalDate.now().atStartOfDay());
            }
            case "WEEK": {
                LocalDate ws = LocalDate.now().with(DayOfWeek.MONDAY);
                LocalDate prev = ws.minusWeeks(1);
                return new Range(prev.atStartOfDay(), ws.atStartOfDay());
            }
            case "MONTH": {
                LocalDate ms = LocalDate.now().withDayOfMonth(1);
                LocalDate prev = ms.minusMonths(1);
                return new Range(prev.atStartOfDay(), ms.atStartOfDay());
            }
            default: return prev(start, end);
        }
    }
}
