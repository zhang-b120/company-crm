package com.qykj.finmaster.analytics.sales.dto;

import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 销售业绩响应体（按 scope 返回不同视图的数据，但共用一个结构）
 */
@Data
public class SalesPerformanceResp {

    /** 实际生效的 scope */
    private String scope;

    /** 核心 KPI */
    private Kpi kpi;

    /** 趋势（按日或按月切片的金额） */
    private List<TrendPointResp> trend;

    /**
     * 排行榜（scope=TEAM 时为团队成员排行、scope=COMPANY 时为销售排行或部门排行）
     * 仅当 scope 不是 PERSONAL 时有值
     */
    private List<RankItem> ranking;

    /**
     * 部门维度（仅 COMPANY 时有值，用于公司领导看各部门对比）
     */
    private List<DeptStat> deptStats;

    @Data
    public static class Kpi {
        /** 当期订单金额 */
        private BigDecimal orderAmount;
        /** 环比增长 % */
        private BigDecimal orderAmountGrowth;
        /** 当期订单数 */
        private Long orderCount;
        private BigDecimal orderCountGrowth;
        /** 客单价 */
        private BigDecimal avgOrderAmount;
        /** 当期新成交客户数（订单关联的唯一 customer_id） */
        private Long dealCustomerCount;
        private BigDecimal dealCustomerCountGrowth;
    }

    @Data
    public static class RankItem {
        @Excel(title = "账号", sort = 2)
        private String username;
        @Excel(title = "姓名", sort = 1)
        private String realName;
        @Excel(title = "订单数", sort = 3)
        private Long orderCount;
        @Excel(title = "订单金额", sort = 4)
        private BigDecimal orderAmount;
        /** 环比增长（金额） */
        @Excel(title = "环比(%)", sort = 5)
        private BigDecimal growth;
        /** 占 scope 总体业绩的百分比 */
        @Excel(title = "贡献度(%)", sort = 6)
        private BigDecimal contributionRate;
    }

    @Data
    public static class DeptStat {
        private Long deptId;
        private String deptName;
        private Long orderCount;
        private BigDecimal orderAmount;
        /** 占全公司业绩的百分比 */
        private BigDecimal contributionRate;
    }
}
