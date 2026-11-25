package com.qykj.finmaster.analytics.order.dto;

import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class OrderAnalyticsResp {

    @Data
    public static class SummaryResp {
        private String scope;
        /** 核心 KPI */
        private Kpi kpi;
        /** 按日或按月的订单金额趋势 */
        private List<TrendPointResp> trend;
        /** 产品结构 */
        private List<ProductMixItem> productMix;
        /** 销售归因排行（按 create_by） */
        private List<SalesAttribution> salesAttribution;
        /** 按订单明细类型（NEW/RENEW/BALANCE）的金额分布 */
        private List<DetailTypeItem> detailTypeDist;
    }

    @Data
    public static class Kpi {
        private BigDecimal orderAmount;
        private BigDecimal orderAmountGrowth;
        private Long orderCount;
        private BigDecimal orderCountGrowth;
        private BigDecimal avgOrderAmount;
        private Long dealCustomerCount;
    }

    @Data
    public static class ProductMixItem {
        private String productName;
        private Long orderCount;
        private BigDecimal amount;
        private BigDecimal amountRate;
    }

    @Data
    public static class SalesAttribution {
        @Excel(title = "账号", sort = 2)
        private String username;
        @Excel(title = "姓名", sort = 1)
        private String realName;
        @Excel(title = "订单数", sort = 3)
        private Long orderCount;
        @Excel(title = "金额", sort = 4)
        private BigDecimal amount;
        @Excel(title = "占比(%)", sort = 5)
        private BigDecimal amountRate;
    }

    @Data
    public static class DetailTypeItem {
        private String type;
        private String typeLabel;
        private Long orderCount;
        private BigDecimal amount;
    }

    /**
     * 订单来源归因：通过 customer_info.lead_id -> sale_lead.source 链式回溯
     */
    @Data
    public static class SourceAttributionItem {
        /** sale_lead.source 字典 code */
        private String source;
        private String sourceLabel;
        private Long orderCount;
        private BigDecimal orderAmount;
        /** 占当期总订单金额的百分比 */
        private BigDecimal amountRate;
    }

    @Data
    public static class RefundResp {
        private String scope;
        /** 当期正常订单数/金额 */
        private Long normalOrderCount;
        private BigDecimal normalOrderAmount;
        /** 当期退款订单数/金额（order_type = REFUND） */
        private Long refundOrderCount;
        private BigDecimal refundAmount;
        /** 退款率 = refundAmount / normalOrderAmount * 100 */
        private BigDecimal refundRate;
        private BigDecimal refundAmountGrowth;
        /** 退款趋势（按日/按月） */
        private List<TrendPointResp> refundTrend;
        /** 责任人排行（退款订单的 create_by） */
        private List<RefundOwnerItem> refundByOwner;
    }

    @Data
    public static class RefundOwnerItem {
        @Excel(title = "账号", sort = 2)
        private String username;
        @Excel(title = "姓名", sort = 1)
        private String realName;
        @Excel(title = "退款单数", sort = 3)
        private Long refundCount;
        @Excel(title = "退款金额", sort = 4)
        private BigDecimal refundAmount;
    }
}
