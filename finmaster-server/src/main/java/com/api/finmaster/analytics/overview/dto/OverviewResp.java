package com.qykj.finmaster.analytics.overview.dto;

import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 综合看板响应体
 */
@Data
public class OverviewResp {

    /** 当前生效的可见层级（回显） */
    private String scope;

    /** 当前账号支持切换到的层级列表 */
    private List<String> availableScopes;

    /** 顶部 KPI 卡片 */
    private KpiBlock kpis;

    /** 线索漏斗 */
    private List<FunnelItem> leadFunnel;

    /** 订单金额趋势 */
    private List<TrendPointResp> orderTrend;

    /** 销售 TOP 5 */
    private List<SalesTopItem> salesTop5;

    @Data
    public static class KpiBlock {
        private Long newLeads;
        private BigDecimal newLeadsGrowth;

        private Long newCustomers;
        private BigDecimal newCustomersGrowth;

        private BigDecimal orderAmount;
        private BigDecimal orderAmountGrowth;

        private Long callCount;
        private BigDecimal callCountGrowth;
    }

    @Data
    public static class FunnelItem {
        /** 1 未跟进 / 2 跟进中 / 3 已转化 / 4 已失效 */
        private Integer status;
        private String statusLabel;
        private Long count;
    }

    @Data
    public static class SalesTopItem {
        private String username;
        private String realName;
        private BigDecimal orderAmount;
        private Long orderCount;
    }
}
