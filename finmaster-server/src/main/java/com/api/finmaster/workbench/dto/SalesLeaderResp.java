package com.qykj.finmaster.workbench.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售团队长工作台数据
 *
 * 两段：
 *   A. 工作进度（实时快照）
 *   B. 工作变化（趋势对比）
 *
 * 每个核心字段都附 byMember 列表，前端弹窗下钻
 */
@Data
public class SalesLeaderResp {

    /** A 段·工作进度 */
    private ProgressBlock progress;

    /** B 段·工作变化 */
    private TrendBlock trend;

    /** 团队成员列表（用于诊断画像下拉） */
    private List<MemberOption> members;

    @Data
    public static class MemberOption {
        private String username;
        private String realName;
    }

    @Data
    public static class ProgressBlock {
        // A1 今日待跟进（拆分线索 / 客户 / 异常）
        private Long todayPendingLeadCount;
        private Long todayPendingCustomerCount;
        /** 异常：填写跟进记录的时间与下次跟进时间在同一天的 lead+customer 数量 */
        private Long abnormalSameDayCount;
        private List<MemberStat> todayPendingByMember;
        /** 详情列表（首屏返回前 200 条） */
        private List<EntityRecord> todayPendingLeadList;
        private List<EntityRecord> todayPendingCustomerList;
        private List<EntityRecord> abnormalRecordList;

        // A2 即将回收（≤3 天）拆分
        private Long aboutRecycleLeadCount;
        private Long aboutRecycleCustomerCount;
        private List<MemberStat> aboutRecycleByMember;
        private List<EntityRecord> aboutRecycleLeadList;
        private List<EntityRecord> aboutRecycleCustomerList;

        // A3 超 N 天未动客户（含 7/14/30 三段总数）
        private Long staleCount7;
        private Long staleCount14;
        private Long staleCount30;
        private List<StaleStat> staleCustomerByMember;

        // A4 今日 0 动作成员
        private Long zeroActionMemberCount;
        private List<MemberAction> zeroActionMembers;

        // A5 在途订单金额（保留字段，前端不展示）
        private BigDecimal pendingOrderAmount;
        private Long pendingOrderCount;
        private List<MemberOrder> pendingOrderByMember;

        // A6 本月退款金额
        private BigDecimal refundAmount;
        private Long refundCount;
        private List<MemberRefund> refundByMember;

        // A9 签单客户数（去重 customer_id）
        private SignCustomerStat signCustomers;

        // A7 三段转化漏斗（线索 → 客户 → 签单）默认本月，按线索创建时间统计
        private FunnelData funnel;

        // A8 客户跟进频次分布（已加 status NOT IN (3,4) 排除已转化/已失效）
        private List<HistogramBucket> followFrequency;
    }

    /** 通用业务记录（线索/客户）*/
    @Data
    public static class EntityRecord {
        private Long id;
        private String name;             // 线索名 / 客户名
        private String contactName;
        private String phone;
        private String ownerUser;
        private String ownerUserName;
        private java.util.Date lastFollowTime;
        private java.util.Date nextFollowTime;
        private java.util.Date createTime;
        /** 距自动回收剩余天数（仅"即将回收"列表用） */
        private Integer remainDays;
        /** 业务类型：'线索' / '客户'（仅异常列表使用，便于区分） */
        private String entityType;
        /** 异常类型：'计划时间无效' / '未按计划跟进'（仅异常列表使用） */
        private String abnormalType;
    }

    @Data
    public static class TrendBlock {
        // B1 业绩排行 TOP10
        private List<PerformanceRank> performanceRanking;
        // B2 6 月业绩走势：team 总线 + TOP5 涨 + TOP5 降（前端勾选展开）
        private List<MonthlySeries> monthlyAmountSeries;
        // B2 本月 vs 上月：榜单（后端预计算，前端不再用 series 推算）
        private List<TopDeltaItem> upMembers;
        private List<TopDeltaItem> downMembers;
        private List<TopDeltaItem> newMembers;
        // B3 掉队/进步榜
        private List<MemberDelta> dropdownMembers;
        private List<MemberDelta> uprisingMembers;
        // B4 团队漏斗（线索→客户→PAID）
        private FunnelData funnel;
        // B5 业绩同环比
        private BigDecimal monthAmount;
        private BigDecimal monthAmountGrowth;
        private BigDecimal monthAmountYoyGrowth;
        /** 本月签单数（PAID 订单数）*/
        private Long monthOrderCount;
        private BigDecimal monthOrderCountGrowth;
        private BigDecimal monthOrderCountYoyGrowth;
        // B6 客单价对比
        private BigDecimal avgOrderAmount;
        private BigDecimal avgOrderAmountGrowth;
        private BigDecimal avgOrderAmountYoyGrowth;
        private List<MemberAvgOrder> avgOrderByMember;
        // B7 业绩构成（按业务类型）
        private List<PieSlice> performanceComposition;
        // B8 来源转化率
        private List<SourceConversion> sourceConversion;
    }

    // ========= 子结构 =========

    @Data
    public static class MemberStat {
        private String username;
        private String realName;
        private Long count;
        /** 同岗位排名 */
        private Integer rank;
    }

    @Data
    public static class StaleStat {
        private String username;
        private String realName;
        private Long count7;
        private Long count14;
        private Long count30;
    }

    @Data
    public static class MemberAction {
        private String username;
        private String realName;
        private Long todayFollowCount;
        private Long todayCallCount;
        private Long todayOrderCount;
    }

    @Data
    public static class MemberOrder {
        private String username;
        private String realName;
        private Long orderCount;
        private BigDecimal amount;
        private BigDecimal avgAgeDays;
    }

    @Data
    public static class MemberRefund {
        private String username;
        private String realName;
        private Long refundCount;
        private BigDecimal refundAmount;
        private BigDecimal refundRate;
    }

    @Data
    public static class FunnelBottleneck {
        private String weakestStage;
        private BigDecimal weakestRate;
        private String hint;
    }

    /** A9 签单客户数（按 today/week/month 三周期） */
    @Data
    public static class SignCustomerStat {
        private Long todayCount;
        private Long weekCount;
        private Long monthCount;
        private List<MemberSignCount> byMember;
    }
    @Data
    public static class MemberSignCount {
        private String username;
        private String realName;
        private Long today;
        private Long week;
        private Long month;
    }

    /** A7 漏斗按期请求（前端独立时间筛选） */
    @Data
    public static class FunnelByPeriodReq {
        private String periodType;       // CURR_MONTH / LAST_MONTH / CUSTOM
        private java.time.LocalDate start;
        private java.time.LocalDate end;
    }

    @Data
    public static class HistogramBucket {
        private String label;
        private Long count;
    }

    @Data
    public static class PerformanceRank {
        private Integer rank;
        private String username;
        private String realName;
        private Long orderCount;
        private BigDecimal orderAmount;
    }

    @Data
    public static class MonthlySeries {
        private String name;
        private String type;     // TEAM / UP / DOWN
        private List<MonthValue> points;
    }

    @Data
    public static class MonthValue {
        private String month;    // 2026-04
        private BigDecimal value;
        /** 仅 6 月业绩走势使用：当月签单数 */
        private Long count;
    }

    @Data
    public static class MemberDelta {
        private String username;
        private String realName;
        private BigDecimal currentValue;
        private BigDecimal previousValue;
        private BigDecimal changePct;
        /** 本月签单数 */
        private Long currentCount;
        /** 上月签单数 */
        private Long previousCount;
    }

    /** B2 走势 6 月分组里附带的本月 vs 上月对比（后端预计算，前端不再瞎算）*/
    @Data
    public static class TopDeltaItem {
        private String username;
        private String realName;
        private BigDecimal delta;       // 百分比，仅 UP/DOWN 用
        private BigDecimal currAmount;  // 本月金额，新晋用
    }

    @Data
    public static class FunnelData {
        private Long leads;
        private Long customers;
        private Long deals;
        private BigDecimal leadToCustomer;
        private BigDecimal customerToDeal;
        private BigDecimal overall;
    }

    @Data
    public static class MemberAvgOrder {
        private String username;
        private String realName;
        private BigDecimal avg;
        private Long orderCount;
    }

    @Data
    public static class PieSlice {
        private String name;
        private BigDecimal value;
        /** 订单数量（业绩构成场景使用） */
        private Long count;
    }

    @Data
    public static class SourceConversion {
        private String source;
        private Long leads;
        private Long converted;
        private BigDecimal rate;
        /** 该来源转化最佳的人 */
        private String topMemberName;
    }

    // ========= 员工诊断画像 =========
    @Data
    public static class EmployeeDiagnosis {
        private String username;
        private String realName;
        private List<MonthValue> amount6m;
        private FunnelData funnel;
        private List<HistogramBucket> dailyAction7d;
        private Long aboutRecycleCount;
        private Long staleCount;
        private Integer rankCurrent;
        private Integer rankPrevious;
    }
}
