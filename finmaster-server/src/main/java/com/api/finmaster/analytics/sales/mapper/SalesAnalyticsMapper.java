package com.qykj.finmaster.analytics.sales.mapper;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.sales.dto.SalesPerformanceResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface SalesAnalyticsMapper {

    /** 区间订单金额合计（PAID） */
    BigDecimal sumOrderAmount(@Param("ctx") AnalyticsContext ctx,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /** 区间订单数 */
    Long countOrders(@Param("ctx") AnalyticsContext ctx,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);

    /** 区间成交客户数（唯一 customer_id） */
    Long countDealCustomers(@Param("ctx") AnalyticsContext ctx,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    /** 按日趋势 */
    List<Map<String, Object>> orderTrendByDay(@Param("ctx") AnalyticsContext ctx,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    /** 按月趋势 */
    List<Map<String, Object>> orderTrendByMonth(@Param("ctx") AnalyticsContext ctx,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    /** 销售人员排行 */
    List<SalesPerformanceResp.RankItem> salesRanking(@Param("ctx") AnalyticsContext ctx,
                                                     @Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    /** 单人订单金额（用于计算环比） */
    BigDecimal salesAmountOfUser(@Param("tenantId") Long tenantId,
                                 @Param("username") String username,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /**
     * 各部门订单金额聚合（COMPANY 层级用）
     * 通过 orders.create_by -> sys_user -> sys_user_department -> sys_department 关联
     */
    List<SalesPerformanceResp.DeptStat> deptStats(@Param("ctx") AnalyticsContext ctx,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);
}
