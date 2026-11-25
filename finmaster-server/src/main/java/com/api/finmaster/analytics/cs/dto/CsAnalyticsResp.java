package com.qykj.finmaster.analytics.cs.dto;

import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class CsAnalyticsResp {

    // ─── 续费分析 ───────────────────────────────────────────────────────────
    @Data
    public static class RenewalResp {
        private String scope;
        /** 到期客户数（end_date 在时间段内） */
        private Long expiringCount;
        /** 已续费数 */
        private Long renewedCount;
        /** 未续费数（renew_type=3） */
        private Long notRenewedCount;
        /** 续费率 = renewed / expiring * 100 */
        private BigDecimal renewalRate;
        /** 续费总金额 */
        private BigDecimal renewAmount;
        /** 续费类型分布 */
        private List<RenewTypeItem> renewTypeDist;
        /** 按管家排行 */
        private List<ButlerRankItem> butlerRanking;
        /** 续费趋势 */
        private List<TrendPointResp> renewTrend;
    }

    @Data
    public static class RenewTypeItem {
        /** 0=正常 1=提前 2=延期 3=未续费 */
        private Integer type;
        private String typeLabel;
        private Long count;
        private BigDecimal amount;
    }

    @Data
    public static class ButlerRankItem {
        @Excel(title = "账号", sort = 2)
        private String butlerUser;
        @Excel(title = "管家", sort = 1)
        private String butlerUserName;
        /** 管理的到期客户数 */
        @Excel(title = "到期客户", sort = 3)
        private Long expiringCount;
        /** 已续费 */
        @Excel(title = "已续费", sort = 4)
        private Long renewedCount;
        /** 续费率 */
        @Excel(title = "续费率(%)", sort = 5)
        private BigDecimal renewalRate;
        /** 续费金额 */
        @Excel(title = "续费金额", sort = 6)
        private BigDecimal renewAmount;
    }

    // ─── CS 任务分析 ────────────────────────────────────────────────────────
    @Data
    public static class TaskSummaryResp {
        private String scope;
        /** 任务总数（当期创建） */
        private Long totalTasks;
        /** 完成数 */
        private Long completedTasks;
        /** 进行中 */
        private Long inProgressTasks;
        /** 已取消 */
        private Long cancelledTasks;
        /** 超期任务数（wish_finish_time < now 且未完成） */
        private Long overdueTasks;
        /** 完成率 */
        private BigDecimal completionRate;
        /** 平均处理时长（天） */
        private BigDecimal avgProcessingDays;
        /** 状态分布 */
        private List<StatusItem> statusDist;
        /** 按处理人排行 */
        private List<TaskOwnerRank> ownerRanking;
    }

    @Data
    public static class StatusItem {
        private String status;
        private Long count;
    }

    @Data
    public static class TaskOwnerRank {
        @Excel(title = "账号", sort = 2)
        private String ownerUser;
        @Excel(title = "处理人", sort = 1)
        private String ownerUserName;
        @Excel(title = "任务数", sort = 3)
        private Long totalTasks;
        @Excel(title = "已完成", sort = 4)
        private Long completedTasks;
        @Excel(title = "超期", sort = 5)
        private Long overdueTasks;
        @Excel(title = "完成率(%)", sort = 6)
        private BigDecimal completionRate;
    }

    // ─── 客户档案 ───────────────────────────────────────────────────────────
    @Data
    public static class CustomerProfileResp {
        /** 星级分布 */
        private List<StarLevelItem> starLevelDist;
        /** 做账情况分布 */
        private List<StringKeyCount> bookkeepingDist;
        /** 报税情况分布 */
        private List<StringKeyCount> filingDist;
    }

    @Data
    public static class StarLevelItem {
        private Integer starLevel;
        private Long count;
    }

    @Data
    public static class StringKeyCount {
        private String name;
        private Long count;
    }

    @Data
    public static class NotRenewReasonsResp {
        /** 未续费原因聚合 */
        private List<StringKeyCount> reasons;
        /** 未续费总数 */
        private Long totalNotRenewed;
        /** 未填原因的占比 */
        private BigDecimal unknownReasonRate;
    }
}
