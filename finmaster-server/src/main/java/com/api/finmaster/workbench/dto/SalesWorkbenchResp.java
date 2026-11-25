package com.qykj.finmaster.workbench.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 销售工作台个性化数据
 *
 * 4 段：① 签单排名（任意时间窗，默认今日）② 我的线索（默认本月）③ 我的客户（默认本月）④ 本月业绩+提成
 * 各段独立时间筛选；每个核心指标含 *Growth（环比 %）/ *YoyGrowth（同比 %）
 */
@Data
public class SalesWorkbenchResp {

    /** 视角：PERSONAL / TEAM */
    private String scope;

    private SignRankingBlock signRanking;
    private LeadStatsBlock leadStats;
    private CustomerStatsBlock customerStats;
    private PerformanceBlock performance;

    @Data
    public static class SignRankingBlock {
        private String periodType;
        private String periodLabel;
        private Long myCount;
        private Integer myRank;
        private Integer totalRanked;
        private Long topCount;
        private Long diffToTop;
        private Long diffToPrev;
        private Long leadOverNext;
        private String topName;
        private String prevName;
        private String nextName;
        /** 环比签单数（%，正数=增长） */
        private BigDecimal myCountGrowth;
        /** 同比签单数（%） */
        private BigDecimal myCountYoyGrowth;
    }

    @Data
    public static class LeadStatsBlock {
        private String periodType;
        private String periodLabel;
        private Long newCount;
        private BigDecimal newCountGrowth;
        private BigDecimal newCountYoyGrowth;
        private Long followedCount;
        private BigDecimal followedCountGrowth;
        private BigDecimal followedCountYoyGrowth;
        private Long aboutToRecycle;
        private BigDecimal conversionRate;
        private BigDecimal conversionRateGrowth;
        private BigDecimal conversionRateYoyGrowth;
    }

    @Data
    public static class CustomerStatsBlock {
        private String periodType;
        private String periodLabel;
        private Long newCount;
        private BigDecimal newCountGrowth;
        private BigDecimal newCountYoyGrowth;
        private Long followedCount;
        private BigDecimal followedCountGrowth;
        private BigDecimal followedCountYoyGrowth;
        private Long aboutToRecycle;
        private BigDecimal dealRate;
        private BigDecimal dealRateGrowth;
        private BigDecimal dealRateYoyGrowth;
    }

    @Data
    public static class PerformanceBlock {
        private BigDecimal currentMonthAmount;
        private BigDecimal currentMonthAmountGrowth;
        private BigDecimal currentMonthAmountYoyGrowth;
        private String currentTierName;
        private BigDecimal currentRate;
        private BigDecimal nextTierThreshold;
        private BigDecimal amountToNextTier;
        private BigDecimal nextTierRate;
        private BigDecimal businessRegProfit;
        private BigDecimal businessRegCommission;
        private BigDecimal performanceCommission;
        private BigDecimal totalCommission;
    }

    /** 兼容 CommissionTierService 旧返回类型 */
    @Data
    public static class CommissionTierBlock {
        private BigDecimal currentMonthAmount;
        private String currentTierName;
        private BigDecimal currentRate;
        private BigDecimal nextTierThreshold;
        private BigDecimal amountToNextTier;
        private BigDecimal nextTierRate;
        private BigDecimal businessRegProfit;
        private BigDecimal businessRegCommission;
        private BigDecimal performanceCommission;
        private BigDecimal totalCommission;
    }
}
