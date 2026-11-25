package com.qykj.finmaster.analytics.common.service;

import com.qykj.finmaster.analytics.common.dto.AnalyticsQueryReq;
import com.qykj.finmaster.analytics.common.enums.CompareMode;
import com.qykj.finmaster.analytics.common.enums.TimeRangeType;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * 将 TimeRangeType 解析为实际时间区间 (start, end)，并根据 CompareMode 计算对比期 (prevStart, prevEnd)
 */
@Component
public class TimeRangeResolver {

    public Range resolveCurrent(AnalyticsQueryReq req) {
        TimeRangeType type = TimeRangeType.fromString(req.getTimeRangeType());
        LocalDate today = LocalDate.now();
        switch (type) {
            case TODAY:
                return new Range(today.atStartOfDay(), today.atTime(LocalTime.MAX));
            case YESTERDAY: {
                LocalDate y = today.minusDays(1);
                return new Range(y.atStartOfDay(), y.atTime(LocalTime.MAX));
            }
            case THIS_WEEK:
                return new Range(
                        today.with(DayOfWeek.MONDAY).atStartOfDay(),
                        today.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX));
            case LAST_WEEK: {
                LocalDate lastMonday = today.with(DayOfWeek.MONDAY).minusWeeks(1);
                LocalDate lastSunday = lastMonday.plusDays(6);
                return new Range(lastMonday.atStartOfDay(), lastSunday.atTime(LocalTime.MAX));
            }
            case THIS_MONTH:
                return new Range(
                        today.withDayOfMonth(1).atStartOfDay(),
                        today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX));
            case LAST_MONTH: {
                LocalDate firstDayOfLastMonth = today.minusMonths(1).withDayOfMonth(1);
                LocalDate lastDayOfLastMonth = firstDayOfLastMonth.with(TemporalAdjusters.lastDayOfMonth());
                return new Range(firstDayOfLastMonth.atStartOfDay(), lastDayOfLastMonth.atTime(LocalTime.MAX));
            }
            case THIS_QUARTER: {
                int quarter = (today.getMonthValue() - 1) / 3;
                LocalDate qStart = LocalDate.of(today.getYear(), quarter * 3 + 1, 1);
                LocalDate qEnd = qStart.plusMonths(3).minusDays(1);
                return new Range(qStart.atStartOfDay(), qEnd.atTime(LocalTime.MAX));
            }
            case THIS_YEAR:
                return new Range(
                        LocalDate.of(today.getYear(), 1, 1).atStartOfDay(),
                        LocalDate.of(today.getYear(), 12, 31).atTime(LocalTime.MAX));
            case LAST_7_DAYS:
                return new Range(today.minusDays(6).atStartOfDay(), today.atTime(LocalTime.MAX));
            case LAST_30_DAYS:
                return new Range(today.minusDays(29).atStartOfDay(), today.atTime(LocalTime.MAX));
            case CUSTOM:
                if (req.getStartTime() == null || req.getEndTime() == null) {
                    // 回退到本月
                    return new Range(
                            today.withDayOfMonth(1).atStartOfDay(),
                            today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX));
                }
                return new Range(req.getStartTime(), req.getEndTime());
            default:
                return new Range(
                        today.withDayOfMonth(1).atStartOfDay(),
                        today.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX));
        }
    }

    public Range resolvePrevious(Range current, CompareMode mode) {
        if (mode == null || mode == CompareMode.NONE || current == null) {
            return null;
        }
        LocalDateTime start = current.getStart();
        LocalDateTime end = current.getEnd();
        long days = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1;
        switch (mode) {
            case WOW:
                return new Range(start.minusDays(days), end.minusDays(days));
            case MOM:
                return new Range(start.minusMonths(1), end.minusMonths(1));
            case YOY:
                return new Range(start.minusYears(1), end.minusYears(1));
            default:
                return null;
        }
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Range {
        private LocalDateTime start;
        private LocalDateTime end;
    }
}
