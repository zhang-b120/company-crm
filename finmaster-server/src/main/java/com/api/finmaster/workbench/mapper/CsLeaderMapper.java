package com.qykj.finmaster.workbench.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Mapper
public interface CsLeaderMapper {

    // ========= A 段 =========

    /** A1 任务积压堆叠（每人按 task_type 数） */
    List<Map<String, Object>> backlogByMember(@Param("tenantId") Long tenantId,
                                              @Param("usernames") List<String> usernames);

    /** A2 超期未办（按管家） */
    List<Map<String, Object>> overdueByMember(@Param("tenantId") Long tenantId,
                                              @Param("usernames") List<String> usernames,
                                              @Param("now") Date now);

    /** A2 超期 TOP 5 详情 */
    List<Map<String, Object>> overdueTop5(@Param("tenantId") Long tenantId,
                                          @Param("usernames") List<String> usernames,
                                          @Param("now") Date now);

    /** A3 超 3 / 7 天未动 */
    List<Map<String, Object>> staleByMember(@Param("tenantId") Long tenantId,
                                            @Param("usernames") List<String> usernames);

    /** A4 到期续费汇总（代账业务，3 桶）*/
    Map<String, Object> expiringRenewalAgg(@Param("usernames") List<String> usernames,
                                           @Param("today") java.time.LocalDate today,
                                           @Param("plus1m") java.time.LocalDate plus1m,
                                           @Param("plus2m") java.time.LocalDate plus2m,
                                           @Param("plus3m") java.time.LocalDate plus3m);

    /** A4 到期续费按管家 */
    List<Map<String, Object>> expiringRenewalByMember(@Param("tenantId") Long tenantId,
                                                       @Param("usernames") List<String> usernames,
                                                       @Param("today") java.time.LocalDate today,
                                                       @Param("plus1m") java.time.LocalDate plus1m,
                                                       @Param("plus2m") java.time.LocalDate plus2m,
                                                       @Param("plus3m") java.time.LocalDate plus3m);

    /** A5 数据收集本月完成率（按管家+总） */
    List<Map<String, Object>> dataCollectionByMember(@Param("tenantId") Long tenantId,
                                                     @Param("usernames") List<String> usernames,
                                                     @Param("period") String period);

    /** A6 管家工作负载 */
    List<Map<String, Object>> memberLoad(@Param("tenantId") Long tenantId,
                                         @Param("usernames") List<String> usernames);

    /** 按 usernames 列表查 sys_user 中所有部门成员（用于诊断画像下拉，全员可选）*/
    List<Map<String, Object>> listMembersByUsernames(@Param("tenantId") Long tenantId,
                                                     @Param("usernames") List<String> usernames);

    /** A7 任务平均停留时间（按状态） */
    List<Map<String, Object>> stageHoldTimes(@Param("tenantId") Long tenantId,
                                             @Param("usernames") List<String> usernames);

    /** A8 续费失败原因聚合 */
    List<Map<String, Object>> notRenewReasons(@Param("tenantId") Long tenantId,
                                              @Param("usernames") List<String> usernames,
                                              @Param("start") Date start,
                                              @Param("end") Date end);

    /** A9 任务驳回率（按管家） */
    List<Map<String, Object>> rejectByMember(@Param("tenantId") Long tenantId,
                                             @Param("usernames") List<String> usernames,
                                             @Param("start") Date start,
                                             @Param("end") Date end);

    // ========= B 段 =========

    /** B1 续费金额按管家（current/prev/yoy 一次返回） */
    List<Map<String, Object>> renewByMember(@Param("tenantId") Long tenantId,
                                            @Param("usernames") List<String> usernames,
                                            @Param("currStart") Date currStart, @Param("currEnd") Date currEnd,
                                            @Param("prevStart") Date prevStart, @Param("prevEnd") Date prevEnd,
                                            @Param("yoyStart") Date yoyStart, @Param("yoyEnd") Date yoyEnd);

    /** B1 团队续费金额（指定区间） */
    BigDecimal teamRenewAmount(@Param("tenantId") Long tenantId,
                               @Param("usernames") List<String> usernames,
                               @Param("start") Date start,
                               @Param("end") Date end);

    /** B2 续费率（团队） */
    Map<String, Object> teamRenewRate(@Param("tenantId") Long tenantId,
                                      @Param("usernames") List<String> usernames,
                                      @Param("start") Date start,
                                      @Param("end") Date end);

    /** B2 续费率按管家 */
    List<Map<String, Object>> renewRateByMember(@Param("tenantId") Long tenantId,
                                                @Param("usernames") List<String> usernames,
                                                @Param("currStart") Date currStart, @Param("currEnd") Date currEnd,
                                                @Param("prevStart") Date prevStart, @Param("prevEnd") Date prevEnd);

    /** B3 6 月续费金额走势（团队总） */
    List<Map<String, Object>> teamRenewMonthly(@Param("tenantId") Long tenantId,
                                               @Param("usernames") List<String> usernames,
                                               @Param("start") Date start,
                                               @Param("end") Date end);

    /** B3 按管家月度 */
    List<Map<String, Object>> memberRenewMonthly(@Param("tenantId") Long tenantId,
                                                 @Param("usernames") List<String> usernames,
                                                 @Param("start") Date start,
                                                 @Param("end") Date end);

    /** B4 完成率排行（任意 task_type） */
    List<Map<String, Object>> completionRanking(@Param("tenantId") Long tenantId,
                                                @Param("usernames") List<String> usernames,
                                                @Param("start") Date start,
                                                @Param("end") Date end);

    /** B5 平均工作时长趋势（团队 6 月） */
    List<Map<String, Object>> teamAvgHourMonthly(@Param("tenantId") Long tenantId,
                                                 @Param("usernames") List<String> usernames,
                                                 @Param("start") Date start,
                                                 @Param("end") Date end);

    /** B6 数据收集 6 个账期完成率 */
    List<Map<String, Object>> dcRateSeries(@Param("tenantId") Long tenantId,
                                           @Param("usernames") List<String> usernames,
                                           @Param("periods") List<String> periods);

    /** B7 本月 vs 上月 完成数对比（按管家） */
    List<Map<String, Object>> doneCountCompare(@Param("tenantId") Long tenantId,
                                               @Param("usernames") List<String> usernames,
                                               @Param("currStart") Date currStart, @Param("currEnd") Date currEnd,
                                               @Param("prevStart") Date prevStart, @Param("prevEnd") Date prevEnd);

    // ========= 诊断画像 =========

    /** 单管家 6 月任务负载（每月 in-flight 数） */
    List<Map<String, Object>> butlerLoadSeries(@Param("tenantId") Long tenantId,
                                               @Param("username") String username,
                                               @Param("start") Date start,
                                               @Param("end") Date end);

    /** 单管家 6 月平均工作时长 */
    List<Map<String, Object>> butlerAvgHourSeries(@Param("tenantId") Long tenantId,
                                                  @Param("username") String username,
                                                  @Param("start") Date start,
                                                  @Param("end") Date end);

    /** 单管家 6 月续费率 */
    List<Map<String, Object>> butlerRenewRateSeries(@Param("tenantId") Long tenantId,
                                                    @Param("username") String username,
                                                    @Param("start") Date start,
                                                    @Param("end") Date end);

    /** 单管家驳回率 */
    Map<String, Object> butlerRejectRate(@Param("tenantId") Long tenantId,
                                         @Param("username") String username,
                                         @Param("start") Date start,
                                         @Param("end") Date end);

    /** 单管家本周到期续费数 */
    Long butlerUpcomingRenewal(@Param("tenantId") Long tenantId,
                               @Param("username") String username,
                               @Param("from") Date from,
                               @Param("to") Date to);

    /** 单管家任务时长（用于直方图） */
    List<Map<String, Object>> butlerTaskDurations(@Param("tenantId") Long tenantId,
                                                  @Param("username") String username,
                                                  @Param("start") Date start,
                                                  @Param("end") Date end);
}
