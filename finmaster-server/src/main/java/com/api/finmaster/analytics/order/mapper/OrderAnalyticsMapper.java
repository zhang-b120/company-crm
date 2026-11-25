package com.qykj.finmaster.analytics.order.mapper;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.order.dto.OrderAnalyticsResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderAnalyticsMapper {

    // ─── Summary ────────────────────────────────────────────────────────────

    BigDecimal sumAmount(@Param("ctx") AnalyticsContext ctx,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    Long countOrders(@Param("ctx") AnalyticsContext ctx,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);

    Long countDealCustomers(@Param("ctx") AnalyticsContext ctx,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    List<Map<String, Object>> trendByDay(@Param("ctx") AnalyticsContext ctx,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    List<Map<String, Object>> trendByMonth(@Param("ctx") AnalyticsContext ctx,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    List<OrderAnalyticsResp.ProductMixItem> productMix(@Param("ctx") AnalyticsContext ctx,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end);

    List<OrderAnalyticsResp.SalesAttribution> salesAttribution(@Param("ctx") AnalyticsContext ctx,
                                                               @Param("start") LocalDateTime start,
                                                               @Param("end") LocalDateTime end);

    List<OrderAnalyticsResp.DetailTypeItem> detailTypeDist(@Param("ctx") AnalyticsContext ctx,
                                                           @Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end);

    // ─── Refund ─────────────────────────────────────────────────────────────

    BigDecimal sumNormalAmount(@Param("ctx") AnalyticsContext ctx,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    Long countNormalOrders(@Param("ctx") AnalyticsContext ctx,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    BigDecimal sumRefundAmount(@Param("ctx") AnalyticsContext ctx,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    Long countRefundOrders(@Param("ctx") AnalyticsContext ctx,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    List<Map<String, Object>> refundTrendByDay(@Param("ctx") AnalyticsContext ctx,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    List<Map<String, Object>> refundTrendByMonth(@Param("ctx") AnalyticsContext ctx,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    List<OrderAnalyticsResp.RefundOwnerItem> refundByOwner(@Param("ctx") AnalyticsContext ctx,
                                                           @Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end);

    /** 订单来源归因（orders -> customer_info.lead_id -> sale_lead.source） */
    List<OrderAnalyticsResp.SourceAttributionItem> sourceAttribution(@Param("ctx") AnalyticsContext ctx,
                                                                     @Param("start") LocalDateTime start,
                                                                     @Param("end") LocalDateTime end);
}
