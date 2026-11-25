package com.qykj.finmaster.analytics.call.mapper;

import com.qykj.finmaster.analytics.call.dto.CallAnalyticsResp;
import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CallAnalyticsMapper {

    Long countTotal(@Param("ctx") AnalyticsContext ctx,
                    @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end);

    Long countConnected(@Param("ctx") AnalyticsContext ctx,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

    Long countNotConnected(@Param("ctx") AnalyticsContext ctx,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    Long sumDuration(@Param("ctx") AnalyticsContext ctx,
                     @Param("start") LocalDateTime start,
                     @Param("end") LocalDateTime end);

    BigDecimal avgDuration(@Param("ctx") AnalyticsContext ctx,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    Long countQualityDone(@Param("ctx") AnalyticsContext ctx,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    BigDecimal avgQualityScore(@Param("ctx") AnalyticsContext ctx,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    List<Map<String, Object>> trendByDay(@Param("ctx") AnalyticsContext ctx,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    List<Map<String, Object>> trendByMonth(@Param("ctx") AnalyticsContext ctx,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    List<CallAnalyticsResp.RankingItem> ranking(@Param("ctx") AnalyticsContext ctx,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    List<CallAnalyticsResp.LowScoreCall> lowScoreCalls(@Param("ctx") AnalyticsContext ctx,
                                                       @Param("start") LocalDateTime start,
                                                       @Param("end") LocalDateTime end,
                                                       @Param("limit") Integer limit);
}
