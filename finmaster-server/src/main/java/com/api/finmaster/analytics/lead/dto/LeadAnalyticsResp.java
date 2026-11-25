package com.qykj.finmaster.analytics.lead.dto;

import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 线索分析响应体集合
 */
public class LeadAnalyticsResp {

    @Data
    public static class FunnelResp {
        private String scope;
        private Long totalLeads;
        private Long unfollowed;   // status=1
        private Long following;    // status=2
        private Long converted;    // status=3
        private Long invalid;      // status=4
        /** 转化率 = converted / totalLeads * 100 */
        private BigDecimal conversionRate;
        /** 有效率 = (total - invalid) / total * 100 */
        private BigDecimal validRate;
    }

    @Data
    public static class SourceRoiItem {
        /** 原始来源值（字典 code） */
        private String source;
        /** 展示名 */
        @Excel(title = "来源", sort = 1)
        private String sourceLabel;
        @Excel(title = "线索数", sort = 2)
        private Long leadCount;
        @Excel(title = "转化数", sort = 3)
        private Long convertedCount;
        /** 转化率 % */
        @Excel(title = "转化率(%)", sort = 4)
        private BigDecimal conversionRate;
        /** 该来源带来的订单金额 */
        @Excel(title = "订单金额", sort = 5)
        private BigDecimal orderAmount;
    }

    @Data
    public static class FollowUpTimingResp {
        /** 平均首次响应时长（小时） */
        private BigDecimal avgFirstResponseHours;
        /** 首次响应时长分布桶 [0-1h, 1-4h, 4-24h, 1-3d, 3-7d, 7d+] 的数量 */
        private List<BucketItem> firstResponseBuckets;
        /** 平均转化周期（天，从创建到 status=3 时间） */
        private BigDecimal avgConversionDays;
        /** 平均每线索跟进次数 */
        private BigDecimal avgFollowUpCount;
    }

    @Data
    public static class BucketItem {
        private String label;
        private Long count;
    }

    @Data
    public static class TrendResp {
        /** 主趋势：新增线索数 */
        private List<TrendPointResp> newLeads;
        /** 辅助：已转化数 */
        private List<TrendPointResp> converted;
        /** 环比增长 */
        private BigDecimal newLeadsGrowth;
        /** 环比增长 */
        private BigDecimal convertedGrowth;
    }
}
