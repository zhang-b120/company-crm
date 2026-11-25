package com.qykj.finmaster.analytics.customer.dto;

import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户分析响应
 */
public class CustomerAnalyticsResp {

    @Data
    public static class PrivateSeaResp {
        private String scope;
        /** 私海总客户数（customer_type = 1） */
        private Long totalCount;
        /** 沉睡客户数：公海里的客户（customer_type = 2）；全系统指标 */
        private Long dormantCount;
        /** 从未跟进客户数：私海里 last_follow_time IS NULL（反映销售触达质量） */
        private Long neverFollowedCount;
        /** 状态分布（私海） */
        private List<StatusItem> statusDist;
        /** 每人私海负载排行 */
        private List<OwnerLoad> ownerLoad;
        /** 私海→成单转化率 % */
        private BigDecimal dealRate;
    }

    @Data
    public static class StatusItem {
        private Integer status;
        private String statusLabel;
        private Long count;
    }

    @Data
    public static class OwnerLoad {
        @Excel(title = "账号", sort = 2)
        private String ownerUser;
        @Excel(title = "负责人", sort = 1)
        private String ownerUserName;
        @Excel(title = "私海客户数", sort = 3)
        private Long customerCount;
        /** 名下从未跟进的客户数（last_follow_time IS NULL） */
        @Excel(title = "从未跟进", sort = 4)
        private Long neverFollowedCount;
        /** 从未跟进占比 = neverFollowedCount / customerCount */
        @Excel(title = "从未跟进占比(%)", sort = 5)
        private BigDecimal neverFollowedRate;
    }

    @Data
    public static class LifecycleResp {
        /** 线索→首单 周期分布 (天) */
        private List<CycleBucket> leadToOrder;
        /** 首单→续费 周期分布 (天) */
        private List<CycleBucket> orderToRenew;
        /** 各阶段客户数 */
        private StageFunnel stageFunnel;
        /** 平均线索→首单 天数 */
        private BigDecimal avgLeadToOrderDays;
        /** 平均首单→续费 天数 */
        private BigDecimal avgOrderToRenewDays;
    }

    @Data
    public static class CycleBucket {
        private String label;
        private Long count;
    }

    @Data
    public static class StageFunnel {
        private Long totalLeads;
        private Long convertedCustomers;  // status=3 客户
        private Long firstOrderCustomers; // 至少一个 PAID 订单
        private Long renewedCustomers;    // 至少一次续费
    }

    // ─── 公海回收分析 ───────────────────────────────────────────────────────
    @Data
    public static class PublicSeaResp {
        private String scope;
        /** 期间转入公海次数（log_type=5） */
        private Long transferInCount;
        /** 期间被领取次数（log_type=6） */
        private Long claimCount;
        /** 期间被分配次数（log_type=7） */
        private Long assignCount;
        /** 回收率 = (claim + assign) / transferIn * 100 */
        private BigDecimal recycleRate;
        /** 平均公海停留天数（从上次转入到最新领取/分配） */
        private BigDecimal avgStayDays;
        /** 当前公海客户总数 */
        private Long currentPublicSeaCount;
        /** 趋势：每日转入 vs 回收 */
        private List<com.qykj.finmaster.analytics.common.dto.TrendPointResp> transferTrend;
        private List<com.qykj.finmaster.analytics.common.dto.TrendPointResp> recycleTrend;
        /** 释放人 TOP 10（谁频繁释放客户） */
        private List<ReleaseItem> topReleasers;
    }

    @Data
    public static class ReleaseItem {
        private String username;
        private String realName;
        private Long releaseCount;
    }
}
