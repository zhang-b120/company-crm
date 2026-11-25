package com.qykj.finmaster.analytics.cs.mapper;

import com.qykj.finmaster.analytics.common.context.AnalyticsContext;
import com.qykj.finmaster.analytics.cs.dto.CsAnalyticsResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface CsAnalyticsMapper {

    // ─── Renewal ────────────────────────────────────────────────────────────

    /** 到期客户数：company_service.end_date BETWEEN start AND end */
    Long countExpiring(@Param("ctx") AnalyticsContext ctx,
                       @Param("start") LocalDate start,
                       @Param("end") LocalDate end);

    /** 已续费客户数：在到期范围内存在 company_service_renew 且 renew_type IN (0,1,2) */
    Long countRenewed(@Param("ctx") AnalyticsContext ctx,
                      @Param("start") LocalDate start,
                      @Param("end") LocalDate end);

    /** 未续费客户数：renew_type=3 */
    Long countNotRenewed(@Param("ctx") AnalyticsContext ctx,
                         @Param("start") LocalDate start,
                         @Param("end") LocalDate end);

    /** 续费总金额 */
    BigDecimal sumRenewAmount(@Param("ctx") AnalyticsContext ctx,
                              @Param("start") LocalDate start,
                              @Param("end") LocalDate end);

    /** 续费类型分布 */
    List<CsAnalyticsResp.RenewTypeItem> renewTypeDist(@Param("ctx") AnalyticsContext ctx,
                                                      @Param("start") LocalDate start,
                                                      @Param("end") LocalDate end);

    /** 管家排行 */
    List<CsAnalyticsResp.ButlerRankItem> butlerRanking(@Param("ctx") AnalyticsContext ctx,
                                                       @Param("start") LocalDate start,
                                                       @Param("end") LocalDate end);

    /** 续费趋势（按月） */
    List<Map<String, Object>> renewTrendByMonth(@Param("ctx") AnalyticsContext ctx,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    /** 续费趋势（按日） */
    List<Map<String, Object>> renewTrendByDay(@Param("ctx") AnalyticsContext ctx,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    // ─── Task ───────────────────────────────────────────────────────────────

    Long countTasks(@Param("ctx") AnalyticsContext ctx,
                    @Param("start") LocalDateTime start,
                    @Param("end") LocalDateTime end,
                    @Param("status") String status);

    /** 当期创建的任务总数 */
    Long countTotalTasks(@Param("ctx") AnalyticsContext ctx,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    /** 当期超期任务（wish_finish_time < now 且 real_finish_time IS NULL 且 status 非完成/取消） */
    Long countOverdueTasks(@Param("ctx") AnalyticsContext ctx,
                           @Param("start") LocalDateTime start,
                           @Param("end") LocalDateTime end);

    /** 平均处理时长（天） */
    BigDecimal avgProcessingDays(@Param("ctx") AnalyticsContext ctx,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    /** 状态分布 */
    List<Map<String, Object>> taskStatusDist(@Param("ctx") AnalyticsContext ctx,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    /** 按处理人排行 */
    List<CsAnalyticsResp.TaskOwnerRank> taskOwnerRanking(@Param("ctx") AnalyticsContext ctx,
                                                         @Param("start") LocalDateTime start,
                                                         @Param("end") LocalDateTime end);

    // ─── Customer Profile ───────────────────────────────────────────────────

    List<CsAnalyticsResp.StarLevelItem> starLevelDist(@Param("ctx") AnalyticsContext ctx);

    List<CsAnalyticsResp.StringKeyCount> bookkeepingDist(@Param("ctx") AnalyticsContext ctx);

    List<CsAnalyticsResp.StringKeyCount> filingDist(@Param("ctx") AnalyticsContext ctx);

    /** 未续费原因聚合（renew_type=3） */
    List<CsAnalyticsResp.StringKeyCount> notRenewReasons(@Param("ctx") AnalyticsContext ctx,
                                                        @Param("start") LocalDate start,
                                                        @Param("end") LocalDate end);

    /** 未续费且原因为空的数量 */
    Long countNotRenewedWithoutReason(@Param("ctx") AnalyticsContext ctx,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);
}
