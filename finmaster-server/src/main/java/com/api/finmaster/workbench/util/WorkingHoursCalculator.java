package com.qykj.finmaster.workbench.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 工作时长计算器
 *
 * 工作时间：周一至周六，每日上午 08:15-12:00（225 分钟） + 下午 14:00-18:00（240 分钟），合计 465 分钟/日
 * 周日为休息日不计入；不考虑法定节假日（如需可后续接入节假日表）
 *
 * 历史调整：
 *   - V2.0.73 (2026-04-29) 初版：下午 13:30-17:30
 *   - 2026-04-29 调整：下午 14:00-18:00（每日工作分钟数不变，仍为 465 / 7.75 小时）
 *
 * 用于计算任务从下发到完成的"实际工作时长"，避免把夜晚/周末算进去
 */
public final class WorkingHoursCalculator {

    private static final LocalTime AM_START = LocalTime.of(8, 15);
    private static final LocalTime AM_END = LocalTime.of(12, 0);
    private static final LocalTime PM_START = LocalTime.of(14, 0);
    private static final LocalTime PM_END = LocalTime.of(18, 0);

    private WorkingHoursCalculator() {}

    /** 计算 [start, end] 区间内的工作分钟数；任一为 null 或 end ≤ start 返回 0 */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0L;
        if (!end.isAfter(start)) return 0L;

        long total = 0L;
        LocalDate startDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            if (cur.getDayOfWeek() != DayOfWeek.SUNDAY) {
                LocalTime dayStart = cur.equals(startDate) ? start.toLocalTime() : LocalTime.MIN;
                LocalTime dayEnd = cur.equals(endDate) ? end.toLocalTime() : LocalTime.MAX;
                total += overlapMinutes(dayStart, dayEnd, AM_START, AM_END);
                total += overlapMinutes(dayStart, dayEnd, PM_START, PM_END);
            }
            cur = cur.plusDays(1);
        }
        return total;
    }

    /** 给定时刻是否落在工作时段内（周一至周六 08:15-12:00 或 14:00-18:00）。null 视为否。 */
    public static boolean isWorkingTime(LocalDateTime dateTime) {
        if (dateTime == null) return false;
        if (dateTime.getDayOfWeek() == DayOfWeek.SUNDAY) return false;
        LocalTime t = dateTime.toLocalTime();
        boolean am = !t.isBefore(AM_START) && t.isBefore(AM_END);
        boolean pm = !t.isBefore(PM_START) && t.isBefore(PM_END);
        return am || pm;
    }

    private static long overlapMinutes(LocalTime s, LocalTime e, LocalTime ws, LocalTime we) {
        LocalTime os = s.isAfter(ws) ? s : ws;
        LocalTime oe = e.isBefore(we) ? e : we;
        if (oe.isAfter(os)) {
            return Duration.between(os, oe).toMinutes();
        }
        return 0L;
    }
}
