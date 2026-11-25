package com.qykj.finmaster.data.service;

import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.data.domain.resp.OrderOverviewItemResp;
import com.qykj.finmaster.data.domain.resp.OrderOverviewResp;
import com.qykj.finmaster.data.mapper.PaymentDataMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据总览服务
 */
@Service
public class OrderOverviewServiceImpl {

    @Autowired
    private PaymentDataMapper paymentDataMapper;

    /**
     * 获取数据总览
     */
    public OrderOverviewResp getOverview() {
        OrderOverviewResp resp = new OrderOverviewResp();
        LocalDate today = LocalDate.now();

        // 1. 查询近7天每日数据（含今天）
        LocalDate weekStart = today.minusDays(6);
        Map<String, Object> dailyParams = new HashMap<>();
        dailyParams.put("start", DateUtils.localDateTimeToDate(weekStart.atStartOfDay()));
        dailyParams.put("end", DateUtils.localDateTimeToDate(today.atTime(LocalTime.MAX)));
        List<OrderOverviewItemResp> dailyDbData = paymentDataMapper.getOrderOverviewDaily(dailyParams);

        // 补全7天数据（没有数据的日期也要显示0）
        Map<String, OrderOverviewItemResp> dailyMap = new HashMap<>();
        if (dailyDbData != null) {
            dailyMap = dailyDbData.stream().collect(Collectors.toMap(OrderOverviewItemResp::getLabel, v -> v));
        }
        List<OrderOverviewItemResp> weeklyData = new ArrayList<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            String key = d.format(dayFmt);
            OrderOverviewItemResp item = dailyMap.getOrDefault(key, new OrderOverviewItemResp());
            item.setLabel(key);
            weeklyData.add(item);
        }
        resp.setWeeklyData(weeklyData);

        // 提取今日数据
        String todayStr = today.format(dayFmt);
        OrderOverviewItemResp todayData = dailyMap.get(todayStr);
        if (todayData != null) {
            resp.setTodayOrderCount(todayData.getOrderCount());
            resp.setTodayOrderAmount(todayData.getOrderAmount());
            resp.setTodayRefundCount(todayData.getRefundCount());
            resp.setTodayRefundAmount(todayData.getRefundAmount());
        }

        // 2. 查询近12个月每月数据（含当月）
        LocalDate yearStart = today.withDayOfMonth(1).minusMonths(11);
        Map<String, Object> monthlyParams = new HashMap<>();
        monthlyParams.put("start", DateUtils.localDateTimeToDate(yearStart.atStartOfDay()));
        monthlyParams.put("end", DateUtils.localDateTimeToDate(today.atTime(LocalTime.MAX)));
        List<OrderOverviewItemResp> monthlyDbData = paymentDataMapper.getOrderOverviewMonthly(monthlyParams);

        // 补全12个月数据
        Map<String, OrderOverviewItemResp> monthlyMap = new HashMap<>();
        if (monthlyDbData != null) {
            monthlyMap = monthlyDbData.stream().collect(Collectors.toMap(OrderOverviewItemResp::getLabel, v -> v));
        }
        List<OrderOverviewItemResp> yearlyData = new ArrayList<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 11; i >= 0; i--) {
            LocalDate m = today.minusMonths(i).withDayOfMonth(1);
            String key = m.format(monthFmt);
            OrderOverviewItemResp item = monthlyMap.getOrDefault(key, new OrderOverviewItemResp());
            item.setLabel(key);
            yearlyData.add(item);
        }
        resp.setYearlyData(yearlyData);

        // 提取当月数据
        String currentMonth = today.format(monthFmt);
        OrderOverviewItemResp monthData = monthlyMap.get(currentMonth);
        if (monthData != null) {
            resp.setMonthOrderCount(monthData.getOrderCount());
            resp.setMonthOrderAmount(monthData.getOrderAmount());
            resp.setMonthRefundCount(monthData.getRefundCount());
            resp.setMonthRefundAmount(monthData.getRefundAmount());
        }

        return resp;
    }
}
