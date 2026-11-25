package com.qykj.finmaster.workbench.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 客服团队长工作台数据 */
@Data
public class CsLeaderResp {

    private ProgressBlock progress;
    private TrendBlock trend;
    private List<MemberOption> members;

    @Data
    public static class MemberOption {
        private String username;
        private String realName;
    }

    @Data
    public static class ProgressBlock {
        // A1 任务积压堆叠柱（按 task_type × 管家）
        private List<StackedBackLog> backlogByMember;
        // A2 超期未办
        private Long overdueTotal;
        private List<OverdueStat> overdueByMember;
        private List<OverdueDetail> overdueTop5;
        // A3 超 3 天未动
        private Long staleTotal;
        private List<StaleStat> staleByMember;
        // A4 到期续费（基于 company_service 代账业务）
        private Long renewWithin1Month;   // 1 月内到期
        private Long renewWithin2Month;   // 2 月内到期（卡片主指标）
        private Long renewWithin3Month;   // 3 月内到期
        private Long renewBucket1m;       // [今天, +1月)
        private Long renewBucket2m;       // [+1月, +2月)
        private Long renewBucket3m;       // [+2月, +3月)
        private List<RenewalStat> upcomingByMember;
        // A5 数据收集本月完成率
        private BigDecimal dataCollectionRate;
        private List<DcStat> dataCollectionByMember;
        // A6 管家工作负载
        private List<MemberLoad> memberLoad;
        // A7 任务平均停留时间
        private List<StageHoldTime> stageHoldTimes;
        // A8 续费失败原因聚合
        private List<PieSlice> notRenewReasons;
        // A9 任务驳回率
        private List<RejectStat> rejectByMember;
    }

    @Data
    public static class TrendBlock {
        // B1 续费金额同环比
        private BigDecimal monthRenewAmount;
        private BigDecimal monthRenewAmountGrowth;
        private BigDecimal monthRenewAmountYoyGrowth;
        private List<MemberRenew> renewByMember;
        // B2 续费率三列对比
        private BigDecimal monthRate;
        private BigDecimal prevMonthRate;
        private BigDecimal yoyRate;
        private List<MemberRate> rateByMember;
        // B3 6 月续费金额走势
        private List<MonthlySeries> renewMonthlySeries;
        // B4 管家完成率排行
        private List<CompletionRank> completionRanking;
        // B5 平均处理时长趋势
        private List<MonthlySeries> avgHourSeries;
        // B6 数据收集完成率走势
        private List<MonthValue> dcRateSeries;
        // B7 掉队/进步管家
        private List<MemberDelta> dropdownMembers;
        private List<MemberDelta> uprisingMembers;
    }

    // ========= 子结构 =========

    @Data
    public static class StackedBackLog {
        private String username;
        private String realName;
        private Long bookkeeping;
        private Long renewal;
        private Long fieldwork;
        private Long dataCollection;
        private Long total;
    }

    @Data
    public static class OverdueStat {
        private String username;
        private String realName;
        private Long overdueCount;
        private Long maxDays;
    }

    @Data
    public static class OverdueDetail {
        private Long workOrderId;
        private String taskNo;
        private String title;
        private String taskType;
        private String ownerName;
        private Long overdueDays;
    }

    @Data
    public static class StaleStat {
        private String username;
        private String realName;
        private Long count3;
        private Long count7;
    }

    @Data
    public static class RenewalStat {
        private String username;
        private String realName;
        private Long bucket1m;   // [今天, +1月)
        private Long bucket2m;   // [+1月, +2月)
        private Long bucket3m;   // [+2月, +3月)
        private Long within2m;   // 2 月内合计
        private Long within3m;   // 3 月内合计
    }

    @Data
    public static class DcStat {
        private String username;
        private String realName;
        private Long totalCompanies;
        private Long doneCompanies;
        private BigDecimal rate;
    }

    @Data
    public static class MemberLoad {
        private String username;
        private String realName;
        private Long activeCount;
    }

    @Data
    public static class StageHoldTime {
        private String stage;
        private BigDecimal avgHours;
        private Long longTailCount;
    }

    @Data
    public static class PieSlice {
        private String name;
        private BigDecimal value;
    }

    @Data
    public static class RejectStat {
        private String username;
        private String realName;
        private Long totalSteps;
        private Long rejectedCount;
        private BigDecimal rejectRate;
    }

    @Data
    public static class MemberRenew {
        private String username;
        private String realName;
        private BigDecimal current;
        private BigDecimal previous;
        private BigDecimal yoy;
        private BigDecimal growth;
        private BigDecimal yoyGrowth;
    }

    @Data
    public static class MemberRate {
        private String username;
        private String realName;
        private BigDecimal current;
        private BigDecimal previous;
    }

    @Data
    public static class MonthlySeries {
        private String name;
        private String type;
        private List<MonthValue> points;
    }

    @Data
    public static class MonthValue {
        private String month;
        private BigDecimal value;
    }

    @Data
    public static class CompletionRank {
        private Integer rank;
        private String username;
        private String realName;
        private Long doneCount;
        private Long totalCount;
        private BigDecimal rate;
    }

    @Data
    public static class MemberDelta {
        private String username;
        private String realName;
        private BigDecimal currentValue;
        private BigDecimal previousValue;
        private BigDecimal changePct;
    }

    // ========= 管家诊断画像 =========
    @Data
    public static class ButlerDiagnosis {
        private String username;
        private String realName;
        private List<MonthValue> loadSeries6m;
        private List<MonthValue> avgHourSeries6m;
        private List<MonthValue> renewRateSeries6m;
        private BigDecimal rejectRate;
        private Long upcomingRenewalCount;
    }
}
