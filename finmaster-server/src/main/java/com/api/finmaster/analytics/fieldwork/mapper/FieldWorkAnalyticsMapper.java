package com.qykj.finmaster.analytics.fieldwork.mapper;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.fieldwork.dto.FieldWorkAnalyticsResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface FieldWorkAnalyticsMapper {

    Long countByStatus(@Param("ctx") AnalyticsContext ctx,
                       @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end,
                       @Param("status") String status);

    Long countTotal(@Param("ctx") AnalyticsContext ctx,
                    @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end);

    BigDecimal avgActualDuration(@Param("ctx") AnalyticsContext ctx,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    BigDecimal avgArrivalDelay(@Param("ctx") AnalyticsContext ctx,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    List<FieldWorkAnalyticsResp.TaskTypeItem> taskTypeDist(@Param("ctx") AnalyticsContext ctx,
                                                           @Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end);

    List<FieldWorkAnalyticsResp.RankItem> assigneeRanking(@Param("ctx") AnalyticsContext ctx,
                                                          @Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end);
}
