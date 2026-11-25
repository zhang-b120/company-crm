package com.qykj.finmaster.analytics.lead.mapper;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.lead.dto.LeadAnalyticsResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface LeadAnalyticsMapper {

    /** 漏斗各 status 计数 */
    List<Map<String, Object>> funnelCounts(@Param("ctx") AnalyticsContext ctx,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    /**
     * 来源 ROI：每个 source 的线索数 / 转化数 / 订单金额
     * 订单金额通过 customer_info.lead_id -> orders(status='PAID') 链接
     */
    List<LeadAnalyticsResp.SourceRoiItem> sourceRoi(@Param("ctx") AnalyticsContext ctx,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    /**
     * 首次跟进响应时长（小时）分桶：计算 crm_lead_log 中 log_type=2 且每条 lead 的最小 create_time 与 sale_lead.create_time 的差
     * 返回 bucket -> count
     */
    List<Map<String, Object>> firstResponseBuckets(@Param("ctx") AnalyticsContext ctx,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    /** 平均首次响应时长（小时） */
    java.math.BigDecimal avgFirstResponseHours(@Param("ctx") AnalyticsContext ctx,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    /** 平均转化周期（天）：对 status=3 的线索，update_time - create_time */
    java.math.BigDecimal avgConversionDays(@Param("ctx") AnalyticsContext ctx,
                                           @Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    /** 平均每线索跟进次数 */
    java.math.BigDecimal avgFollowUpCount(@Param("ctx") AnalyticsContext ctx,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    /** 新增线索趋势（按日） */
    List<Map<String, Object>> newLeadsTrendByDay(@Param("ctx") AnalyticsContext ctx,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    /** 新增线索趋势（按月） */
    List<Map<String, Object>> newLeadsTrendByMonth(@Param("ctx") AnalyticsContext ctx,
                                                   @Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    /** 已转化线索趋势（按日） */
    List<Map<String, Object>> convertedTrendByDay(@Param("ctx") AnalyticsContext ctx,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /** 已转化线索趋势（按月） */
    List<Map<String, Object>> convertedTrendByMonth(@Param("ctx") AnalyticsContext ctx,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end);

    /** 区间总线索数 */
    Long countTotal(@Param("ctx") AnalyticsContext ctx,
                    @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end);

    /** 区间已转化数 */
    Long countConverted(@Param("ctx") AnalyticsContext ctx,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);
}
