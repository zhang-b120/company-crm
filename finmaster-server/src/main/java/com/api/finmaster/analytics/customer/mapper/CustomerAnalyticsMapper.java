package com.qykj.finmaster.analytics.customer.mapper;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.customer.dto.CustomerAnalyticsResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CustomerAnalyticsMapper {

    /** 私海总客户数 */
    Long countPrivateSea(@Param("ctx") AnalyticsContext ctx);

    /**
     * 沉睡客户数：customer_type = 2（公海里的客户）。
     * 全系统指标，不按负责人切分。仅按 tenant 过滤。
     */
    Long countDormant(@Param("ctx") AnalyticsContext ctx);

    /** 从未跟进客户数：私海里 last_follow_time IS NULL */
    Long countNeverFollowed(@Param("ctx") AnalyticsContext ctx);

    /** 状态分布（私海客户） */
    List<Map<String, Object>> statusDistribution(@Param("ctx") AnalyticsContext ctx);

    /** 每人私海负载（含从未跟进数/占比；公海客户不参与统计） */
    List<CustomerAnalyticsResp.OwnerLoad> ownerLoad(@Param("ctx") AnalyticsContext ctx);

    /** 私海成交客户数（存在 PAID 订单的客户） */
    Long countDealCustomers(@Param("ctx") AnalyticsContext ctx);

    /** 线索→首单 周期（天）列表（每个客户一个值） */
    List<java.math.BigDecimal> leadToOrderDays(@Param("ctx") AnalyticsContext ctx);

    /** 首单→首次续费 周期（天）列表 */
    List<java.math.BigDecimal> orderToRenewDays(@Param("ctx") AnalyticsContext ctx);

    /** 各阶段客户计数 */
    Long countTotalLeads(@Param("ctx") AnalyticsContext ctx);
    Long countConvertedCustomers(@Param("ctx") AnalyticsContext ctx);
    Long countFirstOrderCustomers(@Param("ctx") AnalyticsContext ctx);
    Long countRenewedCustomers(@Param("ctx") AnalyticsContext ctx);

    // ─── 公海回收 ───────────────────────────────────────────────────────────

    /** 转入公海次数（log_type=5） */
    Long countTransferIn(@Param("ctx") AnalyticsContext ctx,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    /** 领取次数（log_type=6） */
    Long countClaim(@Param("ctx") AnalyticsContext ctx,
                    @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end);

    /** 分配次数（log_type=7） */
    Long countAssign(@Param("ctx") AnalyticsContext ctx,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);

    /** 当前公海客户总数（customer_type=2） */
    Long countCurrentPublicSea(@Param("ctx") AnalyticsContext ctx);

    /** 平均停留天数：对每一对 (最近转入 → 最近领取/分配) 计算天数均值 */
    java.math.BigDecimal avgStayDays(@Param("ctx") AnalyticsContext ctx,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    /** 转入公海趋势（按日） */
    List<Map<String, Object>> transferTrendByDay(@Param("ctx") AnalyticsContext ctx,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    /** 转入公海趋势（按月） */
    List<Map<String, Object>> transferTrendByMonth(@Param("ctx") AnalyticsContext ctx,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    /** 回收趋势（按日） */
    List<Map<String, Object>> recycleTrendByDay(@Param("ctx") AnalyticsContext ctx,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    /** 回收趋势（按月） */
    List<Map<String, Object>> recycleTrendByMonth(@Param("ctx") AnalyticsContext ctx,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /** 释放人 TOP 10 */
    List<CustomerAnalyticsResp.ReleaseItem> topReleasers(@Param("ctx") AnalyticsContext ctx,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);
}
