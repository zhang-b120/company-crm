package com.qykj.finmaster.analytics.oa.mapper;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.oa.dto.OaAnalyticsResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface OaAnalyticsMapper {

    List<Map<String, Object>> statusDistribution(@Param("ctx") AnalyticsContext ctx,
                                                 @Param("start") LocalDate start,
                                                 @Param("end") LocalDate end);

    List<OaAnalyticsResp.UserCompliance> worstComplianceUsers(@Param("ctx") AnalyticsContext ctx,
                                                              @Param("start") LocalDate start,
                                                              @Param("end") LocalDate end);

    /** 请假总天数（status=APPROVED） */
    java.math.BigDecimal sumLeaveDays(@Param("ctx") AnalyticsContext ctx,
                                      @Param("start") java.time.LocalDateTime start,
                                      @Param("end") java.time.LocalDateTime end);

    Long countApprovedLeaves(@Param("ctx") AnalyticsContext ctx,
                             @Param("start") java.time.LocalDateTime start,
                             @Param("end") java.time.LocalDateTime end);

    List<OaAnalyticsResp.LeaveTypeItem> leaveTypeDist(@Param("ctx") AnalyticsContext ctx,
                                                      @Param("start") java.time.LocalDateTime start,
                                                      @Param("end") java.time.LocalDateTime end);

    java.math.BigDecimal sumOvertimeHours(@Param("ctx") AnalyticsContext ctx,
                                          @Param("start") java.time.LocalDateTime start,
                                          @Param("end") java.time.LocalDateTime end);

    Long countOvertime(@Param("ctx") AnalyticsContext ctx,
                       @Param("start") java.time.LocalDateTime start,
                       @Param("end") java.time.LocalDateTime end);

    List<OaAnalyticsResp.UserLeaveItem> topLeaveUsers(@Param("ctx") AnalyticsContext ctx,
                                                      @Param("start") java.time.LocalDateTime start,
                                                      @Param("end") java.time.LocalDateTime end);
}
