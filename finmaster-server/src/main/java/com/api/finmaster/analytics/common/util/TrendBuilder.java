package com.qykj.finmaster.analytics.common.util;

import com.qykj.finmaster.analytics.common.dto.TrendPointResp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 按日/月粒度将 [start, end] 时间段切片，并从传入的 {label: value} 稀疏 map 中补齐空白点
 */
public class TrendBuilder {

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public enum Granularity { DAY, MONTH }

    /**
     * 根据时间跨度自动选择粒度：<= 31 天用 DAY，否则用 MONTH
     */
    public static Granularity pickGranularity(LocalDateTime start, LocalDateTime end) {
        long days = ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate()) + 1;
        return days <= 31 ? Granularity.DAY : Granularity.MONTH;
    }

    /**
     * 填平时间序列。sparseData 的 key 必须和 pickGranularity 产生的 label 一致。
     */
    public static List<TrendPointResp> fill(LocalDateTime start, LocalDateTime end,
                                            Granularity g, Map<String, BigDecimal> sparseValue,
                                            Map<String, Long> sparseCount) {
        List<TrendPointResp> out = new ArrayList<>();
        if (start == null || end == null || start.isAfter(end)) {
            return out;
        }
        if (sparseValue == null) sparseValue = new HashMap<>();
        if (sparseCount == null) sparseCount = new HashMap<>();
        if (g == Granularity.DAY) {
            LocalDate cur = start.toLocalDate();
            LocalDate endDay = end.toLocalDate();
            while (!cur.isAfter(endDay)) {
                String label = cur.format(DAY_FMT);
                out.add(new TrendPointResp(
                        label,
                        sparseValue.getOrDefault(label, BigDecimal.ZERO),
                        sparseCount.getOrDefault(label, 0L)));
                cur = cur.plusDays(1);
            }
        } else {
            LocalDate cur = start.toLocalDate().withDayOfMonth(1);
            LocalDate endMonth = end.toLocalDate().withDayOfMonth(1);
            while (!cur.isAfter(endMonth)) {
                String label = cur.format(MONTH_FMT);
                out.add(new TrendPointResp(
                        label,
                        sparseValue.getOrDefault(label, BigDecimal.ZERO),
                        sparseCount.getOrDefault(label, 0L)));
                cur = cur.plusMonths(1);
            }
        }
        return out;
    }

    /**
     * 计算同环比百分比，返回 null 表示基数为 0 或 prev 不存在（前端可展示 "-"）
     * 返回值保留 2 位小数
     */
    public static BigDecimal growthPercent(BigDecimal current, BigDecimal prev) {
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) return null;
        if (current == null) current = BigDecimal.ZERO;
        return current.subtract(prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(prev, 2, java.math.RoundingMode.HALF_UP);
    }

    public static BigDecimal growthPercent(Long current, Long prev) {
        if (prev == null || prev == 0L) return null;
        long c = current == null ? 0L : current;
        return BigDecimal.valueOf(c - prev)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(prev), 2, java.math.RoundingMode.HALF_UP);
    }
}
