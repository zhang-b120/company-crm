package com.qykj.finmaster.data.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.data.domain.resp.BossAnalyticsResp;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BossAnalyticsService {

    @Autowired private LeadManagerImpl leadManager;
    @Autowired private CustomerInfoManagerImpl customerInfoManager;
    @Autowired private OrderManagerImpl orderManager;

    public BossAnalyticsResp getAnalytics(Long tenantId) {
        BossAnalyticsResp resp = new BossAnalyticsResp();
        LocalDate today = LocalDate.now();
        Date todayStart = toDate(today.atStartOfDay());
        Date todayEnd   = toDate(today.atTime(LocalTime.MAX));
        Date weekStart  = toDate(today.minusDays(6).atStartOfDay());
        Date monthStart = toDate(today.withDayOfMonth(1).atStartOfDay());

        // ─── 线索统计 ──────────────────────────────────────────────────────────
        resp.setTodayNewLeads(countLeads(tenantId, todayStart, todayEnd, null));
        resp.setWeekNewLeads(countLeads(tenantId, weekStart, todayEnd, null));
        resp.setMonthNewLeads(countLeads(tenantId, monthStart, todayEnd, null));
        resp.setTotalLeads(countLeads(tenantId, null, null, null));
        resp.setConvertedLeads(countLeads(tenantId, null, null, 3));

        // ─── 客户统计 ──────────────────────────────────────────────────────────
        resp.setTotalCustomers(countCustomers(tenantId, null));
        resp.setActiveCustomers(countCustomers(tenantId, 2)); // 跟进中

        // ─── 订单统计 ──────────────────────────────────────────────────────────
        resp.setTodayNewOrders(countOrders(tenantId, todayStart, todayEnd));
        resp.setWeekNewOrders(countOrders(tenantId, weekStart, todayEnd));
        resp.setMonthNewOrders(countOrders(tenantId, monthStart, todayEnd));
        resp.setTotalOrders(countOrders(tenantId, null, null));

        // ─── 收入统计 ──────────────────────────────────────────────────────────
        resp.setTodayRevenue(sumRevenue(tenantId, todayStart, todayEnd));
        resp.setWeekRevenue(sumRevenue(tenantId, weekStart, todayEnd));
        resp.setMonthRevenue(sumRevenue(tenantId, monthStart, todayEnd));
        resp.setTotalRevenue(sumRevenue(tenantId, null, null));

        // ─── 近7天趋势 ────────────────────────────────────────────────────────
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<BossAnalyticsResp.DailyTrend> trends = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            Date ds = toDate(d.atStartOfDay());
            Date de = toDate(d.atTime(LocalTime.MAX));
            BossAnalyticsResp.DailyTrend trend = new BossAnalyticsResp.DailyTrend();
            trend.setDate(d.format(fmt));
            trend.setNewLeads(countLeads(tenantId, ds, de, null));
            trend.setNewOrders(countOrders(tenantId, ds, de));
            trend.setRevenue(sumRevenue(tenantId, ds, de));
            trends.add(trend);
        }
        resp.setWeeklyTrend(trends);

        return resp;
    }

    private Long countLeads(Long tenantId, Date start, Date end, Integer status) {
        LambdaQueryWrapper<Lead> w = new LambdaQueryWrapper<Lead>()
                .eq(Lead::getTenantId, tenantId).eq(Lead::getDeleted, 0);
        if (start != null) w.ge(Lead::getCreateTime, start);
        if (end   != null) w.le(Lead::getCreateTime, end);
        if (status != null) w.eq(Lead::getStatus, status);
        return leadManager.count(w);
    }

    private Long countCustomers(Long tenantId, Integer status) {
        LambdaQueryWrapper<CustomerInfo> w = new LambdaQueryWrapper<CustomerInfo>()
                .eq(CustomerInfo::getTenantId, tenantId)
                .eq(CustomerInfo::getCustomerType, 1);
        if (status != null) w.eq(CustomerInfo::getStatus, status);
        return customerInfoManager.count(w);
    }

    private Long countOrders(Long tenantId, Date start, Date end) {
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tenantId);
        if (start != null) w.ge(Order::getCreateTime, start);
        if (end   != null) w.le(Order::getCreateTime, end);
        return orderManager.count(w);
    }

    private BigDecimal sumRevenue(Long tenantId, Date start, Date end) {
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<Order>()
                .eq(Order::getTenantId, tenantId)
                .select(Order::getTotalAmount);
        if (start != null) w.ge(Order::getCreateTime, start);
        if (end   != null) w.le(Order::getCreateTime, end);
        List<Order> orders = orderManager.list(w);
        return orders.stream()
                .map(Order::getTotalAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Date toDate(LocalDateTime ldt) {
        return java.util.Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant());
    }
}
