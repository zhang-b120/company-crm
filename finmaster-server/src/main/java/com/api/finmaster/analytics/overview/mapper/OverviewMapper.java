package com.qykj.finmaster.analytics.overview.mapper;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.overview.dto.OverviewResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Mapper
public interface OverviewMapper {

    /** 区间内新增线索数 */
    Long countNewLeads(@Param("ctx") AnalyticsContext ctx,
                       @Param("start") java.time.LocalDateTime start,
                       @Param("end") java.time.LocalDateTime end);

    /** 区间内新增私海客户数（customer_type=1） */
    Long countNewCustomers(@Param("ctx") AnalyticsContext ctx,
                           @Param("start") java.time.LocalDateTime start,
                           @Param("end") java.time.LocalDateTime end);

    /** 区间内已付款订单金额合计（按 create_time 取） */
    BigDecimal sumOrderAmount(@Param("ctx") AnalyticsContext ctx,
                              @Param("start") java.time.LocalDateTime start,
                              @Param("end") java.time.LocalDateTime end);

    /** 区间内通话记录数 */
    Long countCalls(@Param("ctx") AnalyticsContext ctx,
                    @Param("start") java.time.LocalDateTime start,
                    @Param("end") java.time.LocalDateTime end);

    /** 线索漏斗：按 status 分组计数 */
    List<Map<String, Object>> leadFunnel(@Param("ctx") AnalyticsContext ctx,
                                         @Param("start") java.time.LocalDateTime start,
                                         @Param("end") java.time.LocalDateTime end);

    /** 订单趋势：按日聚合金额（要求 end - start <= 31 天） */
    List<Map<String, Object>> orderTrendByDay(@Param("ctx") AnalyticsContext ctx,
                                              @Param("start") java.time.LocalDateTime start,
                                              @Param("end") java.time.LocalDateTime end);

    /** 订单趋势：按月聚合金额 */
    List<Map<String, Object>> orderTrendByMonth(@Param("ctx") AnalyticsContext ctx,
                                                @Param("start") java.time.LocalDateTime start,
                                                @Param("end") java.time.LocalDateTime end);

    /** 销售 TOP N：按订单金额降序 */
    List<OverviewResp.SalesTopItem> salesTopN(@Param("ctx") AnalyticsContext ctx,
                                              @Param("start") java.time.LocalDateTime start,
                                              @Param("end") java.time.LocalDateTime end,
                                              @Param("topN") Integer topN);
}
