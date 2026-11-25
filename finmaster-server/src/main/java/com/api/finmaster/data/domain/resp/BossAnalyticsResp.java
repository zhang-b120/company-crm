package com.qykj.finmaster.data.domain.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BossAnalyticsResp {

    // 线索统计
    private Long todayNewLeads;
    private Long weekNewLeads;
    private Long monthNewLeads;
    private Long totalLeads;
    private Long convertedLeads;    // 已转化线索数

    // 订单统计
    private Long todayNewOrders;
    private Long weekNewOrders;
    private Long monthNewOrders;
    private Long totalOrders;

    // 收入统计（订单金额，非已收款）
    private BigDecimal todayRevenue;
    private BigDecimal weekRevenue;
    private BigDecimal monthRevenue;
    private BigDecimal totalRevenue;

    // 私海客户统计
    private Long totalCustomers;
    private Long activeCustomers;   // 跟进中的客户

    // 近7天每日趋势
    private List<DailyTrend> weeklyTrend;

    @Data
    public static class DailyTrend {
        private String date;
        private Long newLeads;
        private Long newOrders;
        private BigDecimal revenue;
    }
}
