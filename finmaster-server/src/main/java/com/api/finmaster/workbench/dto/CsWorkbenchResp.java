package com.qykj.finmaster.workbench.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 客服工作台个性化数据 (V2.0.70 起新增)
 *
 * 三段式：客户对接（代理记账任务）/ 续费 / 数据收集（占位）
 * 全部以登录用户为视角（current_owner_user 或 owner_user = me）
 */
@Data
public class CsWorkbenchResp {

    /** 视角：PERSONAL / TEAM */
    private String scope;

    /** 客户对接：BOOKKEEPING 任务 */
    private TaskGroup bookkeeping;

    /** 续费：RENEWAL 任务 */
    private RenewalGroup renewal;

    /** 数据收集：DATA_COLLECTION 任务（按"任务×企业"对数粒度） */
    private DataCollectionGroup dataCollection;

    /** 外勤任务：FIELD_WORK 任务（与对接同结构） */
    private TaskGroup fieldWork;

    @Data
    public static class TaskGroup {
        private Long total;
        private BigDecimal totalGrowth;
        private BigDecimal totalYoyGrowth;
        private Long todayDue;
        private Long staleCount;
        private BigDecimal staleCountGrowth;
        private BigDecimal staleCountYoyGrowth;
        private Long monthDone;
        private BigDecimal monthDoneGrowth;
        private BigDecimal monthDoneYoyGrowth;
        private Long monthNew;
        private BigDecimal monthNewGrowth;
        private BigDecimal monthNewYoyGrowth;
        private BigDecimal monthCompletionRate;
        private BigDecimal monthAvgWorkingHours;
        private RankingInfo monthRanking;
    }

    @Data
    public static class RenewalGroup {
        private Long total;
        private BigDecimal totalGrowth;
        private BigDecimal totalYoyGrowth;
        private Long monthDue;
        private Long overdue;
        private BigDecimal overdueGrowth;
        private BigDecimal overdueYoyGrowth;
        private Long monthDone;
        private BigDecimal monthDoneGrowth;
        private BigDecimal monthDoneYoyGrowth;
        private BigDecimal monthRenewalRate;
        private BigDecimal monthRenewAmount;
        private BigDecimal monthRenewAmountGrowth;
        private BigDecimal monthRenewAmountYoyGrowth;
        private BigDecimal monthAvgWorkingHours;
        private RankingInfo monthRanking;
    }

    /** 月度完成数排名信息（按 current_owner_user 聚合 status=DONE 任务数） */
    @Data
    public static class RankingInfo {
        /** 排名指标：COUNT=按完成数，RATE=按完成率（百分比） */
        private String metric = "COUNT";
        /** 我的排名（1 起；null=未上榜） */
        private Integer myRank;
        /** 上榜总人数 */
        private Integer totalRanked;

        // ─── COUNT 模式字段 ────────────────────────────────────────
        /** 我的本月完成数 */
        private Long myCount;
        /** 距榜首还差（≥0） */
        private Long diffToTop;
        /** 比前一名少多少（rank=1 时为 0） */
        private Long diffToPrev;
        /** 比后一名多多少（rank=末尾时为 null） */
        private Long leadOverNext;

        // ─── RATE 模式字段（百分比，2 位小数） ─────────────────────
        /** 我的完成率 */
        private BigDecimal myRate;
        /** 榜首完成率 */
        private BigDecimal topRate;
        /** 距榜首相差百分点（≥0） */
        private BigDecimal diffToTopRate;
        /** 比前一名少的百分点 */
        private BigDecimal diffToPrevRate;
        /** 比后一名多的百分点 */
        private BigDecimal leadOverNextRate;

        // ─── 名字字段（保留供后端 toString，前端不展示）─────────────
        private String topName;
        private String prevName;
        private String nextName;
    }

    @Data
    public static class DataCollectionGroup {
        /** 当前统计的账期（yyyy-MM-dd，前端传入或后端取默认） */
        private String period;
        /** 完成率环比 / 同比 */
        private BigDecimal completionRateGrowth;
        private BigDecimal completionRateYoyGrowth;
        private BigDecimal doneGrowth;
        private BigDecimal doneYoyGrowth;
        /** 我管辖的企业总数（按 company_service.butler_owner_user = me 去重） */
        private Long total;
        /** 已完成企业数（已上传银行流水或标记本期无流水）—— 兼容历史字段 */
        private Long done;
        /** 本月完成的企业数（统计窗口内的"任务×企业"完成对数） */
        private Long monthDone;
        /** 累计完成的企业数（不限时间） */
        private Long totalDone;
        /** 完成率（百分比，2 位小数）= 完成 / 总数 */
        private BigDecimal completionRate;
        /** 全员最新任务完成率排名 */
        private RankingInfo monthRanking;
    }
}
