package com.qykj.finmaster.workbench.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 销售团队长工作台 Mapper
 *
 * 全部 SQL 接受团队成员 username 列表（IN）+ 时间范围
 * 部分查询同时返回团队总值与按成员明细行
 */
@Mapper
public interface SalesLeaderMapper {

    // ========= A 段·进度 =========

    /** A1 今日待跟进总数（lead+customer 合并） */
    Long countTodayPendingTotal(@Param("tenantId") Long tenantId,
                                @Param("usernames") List<String> usernames,
                                @Param("dayStart") LocalDateTime dayStart,
                                @Param("dayEnd") LocalDateTime dayEnd);

    /** A1 今日待跟进 - 仅线索 */
    Long countTodayPendingLead(@Param("tenantId") Long tenantId,
                               @Param("usernames") List<String> usernames,
                               @Param("dayStart") LocalDateTime dayStart,
                               @Param("dayEnd") LocalDateTime dayEnd);

    /** A1 今日待跟进 - 仅客户 */
    Long countTodayPendingCustomer(@Param("tenantId") Long tenantId,
                                   @Param("usernames") List<String> usernames,
                                   @Param("dayStart") LocalDateTime dayStart,
                                   @Param("dayEnd") LocalDateTime dayEnd);

    /** A1 异常：填写跟进时间 = 下次跟进时间（同一天） */
    Long countAbnormalSameDay(@Param("tenantId") Long tenantId,
                              @Param("usernames") List<String> usernames);

    /** A1 详情：今日待跟进线索 list */
    List<Map<String, Object>> listTodayPendingLeads(@Param("tenantId") Long tenantId,
                                                    @Param("usernames") List<String> usernames,
                                                    @Param("dayStart") LocalDateTime dayStart,
                                                    @Param("dayEnd") LocalDateTime dayEnd);

    /** A1 详情：今日待跟进客户 list */
    List<Map<String, Object>> listTodayPendingCustomers(@Param("tenantId") Long tenantId,
                                                        @Param("usernames") List<String> usernames,
                                                        @Param("dayStart") LocalDateTime dayStart,
                                                        @Param("dayEnd") LocalDateTime dayEnd);

    /** A1 详情：异常记录 list（lead + customer 合并） */
    List<Map<String, Object>> listAbnormalRecords(@Param("tenantId") Long tenantId,
                                                  @Param("usernames") List<String> usernames);

    /** A1 按成员拆分 */
    List<Map<String, Object>> todayPendingByMember(@Param("tenantId") Long tenantId,
                                                   @Param("usernames") List<String> usernames,
                                                   @Param("dayStart") LocalDateTime dayStart,
                                                   @Param("dayEnd") LocalDateTime dayEnd);

    /** A2 即将回收（lead+customer 合并）；salesDeptUsernames 用于线索部分与 LeadAutoInvalidateTask 对齐 */
    Long countAboutRecycle(@Param("tenantId") Long tenantId,
                           @Param("usernames") List<String> usernames,
                           @Param("leadFrom") Integer leadFrom, @Param("leadTo") Integer leadTo,
                           @Param("custFrom") Integer custFrom, @Param("custTo") Integer custTo,
                           @Param("salesDeptUsernames") List<String> salesDeptUsernames);

    /** A2 即将回收 - 仅线索 */
    Long countAboutRecycleLead(@Param("tenantId") Long tenantId,
                               @Param("usernames") List<String> usernames,
                               @Param("warnFromDays") Integer warnFromDays,
                               @Param("warnToDays") Integer warnToDays,
                               @Param("salesDeptUsernames") List<String> salesDeptUsernames);

    /** A2 即将回收 - 仅客户 */
    Long countAboutRecycleCustomer(@Param("tenantId") Long tenantId,
                                   @Param("usernames") List<String> usernames,
                                   @Param("warnFromDays") Integer warnFromDays,
                                   @Param("warnToDays") Integer warnToDays);

    /** A2 详情：即将回收线索 list */
    List<Map<String, Object>> listAboutRecycleLeads(@Param("tenantId") Long tenantId,
                                                    @Param("usernames") List<String> usernames,
                                                    @Param("warnFromDays") Integer warnFromDays,
                                                    @Param("warnToDays") Integer warnToDays,
                                                    @Param("threshold") Integer threshold,
                                                    @Param("salesDeptUsernames") List<String> salesDeptUsernames);

    /** A2 详情：即将回收客户 list */
    List<Map<String, Object>> listAboutRecycleCustomers(@Param("tenantId") Long tenantId,
                                                        @Param("usernames") List<String> usernames,
                                                        @Param("warnFromDays") Integer warnFromDays,
                                                        @Param("warnToDays") Integer warnToDays,
                                                        @Param("threshold") Integer threshold);

    /** A2 按成员；salesDeptUsernames 用于线索部分与 LeadAutoInvalidateTask 对齐 */
    List<Map<String, Object>> aboutRecycleByMember(@Param("tenantId") Long tenantId,
                                                   @Param("usernames") List<String> usernames,
                                                   @Param("leadFrom") Integer leadFrom, @Param("leadTo") Integer leadTo,
                                                   @Param("custFrom") Integer custFrom, @Param("custTo") Integer custTo,
                                                   @Param("salesDeptUsernames") List<String> salesDeptUsernames);

    /** A3 超 7/14/30 天未动客户分布按成员 */
    List<Map<String, Object>> staleCustomerByMember(@Param("tenantId") Long tenantId,
                                                    @Param("usernames") List<String> usernames);

    /** A3 总数三段聚合（用于 stat-box 副标） */
    Map<String, Object> staleCustomerSegments(@Param("tenantId") Long tenantId,
                                              @Param("usernames") List<String> usernames);

    /** A7 三段漏斗（线索 → 客户 → 签单），按 lead.create_time 在区间 */
    Map<String, Object> teamFunnelByLeadTime(@Param("tenantId") Long tenantId,
                                             @Param("usernames") List<String> usernames,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    /** A7 漏斗按成员明细 */
    List<Map<String, Object>> teamFunnelByMember(@Param("tenantId") Long tenantId,
                                                  @Param("usernames") List<String> usernames,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /** 按 usernames 查 sys_user 中所有部门成员（员工诊断画像下拉用，含没有业绩的人）*/
    List<Map<String, Object>> listMembersByUsernames(@Param("tenantId") Long tenantId,
                                                     @Param("usernames") List<String> usernames);

    /** A9 签单客户数（按区间，customer 去重） */
    Long countSignCustomersByPeriod(@Param("tenantId") Long tenantId,
                                    @Param("usernames") List<String> usernames,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    /** A9 签单客户按成员（today / week / month 各自的去重客户数） */
    List<Map<String, Object>> signCustomersByMember(@Param("tenantId") Long tenantId,
                                                    @Param("usernames") List<String> usernames,
                                                    @Param("dayStart") LocalDateTime dayStart,
                                                    @Param("dayEnd") LocalDateTime dayEnd,
                                                    @Param("weekStart") LocalDateTime weekStart,
                                                    @Param("weekEnd") LocalDateTime weekEnd,
                                                    @Param("monthStart") LocalDateTime monthStart,
                                                    @Param("monthEnd") LocalDateTime monthEnd);

    /** A4 今日 0 动作成员（每人今日跟进/通话/订单数） */
    List<Map<String, Object>> memberDailyAction(@Param("tenantId") Long tenantId,
                                                @Param("usernames") List<String> usernames,
                                                @Param("dayStart") LocalDateTime dayStart,
                                                @Param("dayEnd") LocalDateTime dayEnd);

    /** A5 在途订单聚合 */
    Map<String, Object> pendingOrderAgg(@Param("tenantId") Long tenantId,
                                        @Param("usernames") List<String> usernames);

    /** A5 在途订单按成员 */
    List<Map<String, Object>> pendingOrderByMember(@Param("tenantId") Long tenantId,
                                                   @Param("usernames") List<String> usernames);

    /** A6 本月退款金额聚合 */
    Map<String, Object> refundAgg(@Param("tenantId") Long tenantId,
                                  @Param("usernames") List<String> usernames,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end);

    /** A6 退款按成员 */
    List<Map<String, Object>> refundByMember(@Param("tenantId") Long tenantId,
                                             @Param("usernames") List<String> usernames,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    /** A8 客户跟进频次直方图：上次跟进距今天数分桶 */
    List<Map<String, Object>> followFrequencyHistogram(@Param("tenantId") Long tenantId,
                                                       @Param("usernames") List<String> usernames);

    // ========= B 段·变化 =========

    /** B1 业绩排行 TOP10（本月 PAID by create_by） */
    List<Map<String, Object>> performanceRanking(@Param("tenantId") Long tenantId,
                                                 @Param("usernames") List<String> usernames,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end,
                                                 @Param("limit") Integer limit);

    /** B2 6 月业绩走势（团队总） */
    List<Map<String, Object>> teamMonthlyAmount(@Param("tenantId") Long tenantId,
                                                @Param("usernames") List<String> usernames,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end);

    /** B2 6 月业绩走势（按成员） */
    List<Map<String, Object>> memberMonthlyAmount(@Param("tenantId") Long tenantId,
                                                  @Param("usernames") List<String> usernames,
                                                  @Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end);

    /** B3 本月 vs 上月每人业绩对比（用于掉队/进步榜） */
    List<Map<String, Object>> memberAmountCompare(@Param("tenantId") Long tenantId,
                                                  @Param("usernames") List<String> usernames,
                                                  @Param("currStart") LocalDateTime currStart,
                                                  @Param("currEnd") LocalDateTime currEnd,
                                                  @Param("prevStart") LocalDateTime prevStart,
                                                  @Param("prevEnd") LocalDateTime prevEnd);

    /** B4 团队漏斗：本月 lead/customer/deal 数 */
    Map<String, Object> teamFunnel(@Param("tenantId") Long tenantId,
                                   @Param("usernames") List<String> usernames,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    /** B5 团队业绩同环比所需的金额（使用本月、上月、去年同月单独查） */
    BigDecimal teamSignAmount(@Param("tenantId") Long tenantId,
                              @Param("usernames") List<String> usernames,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /** B5 团队签单数（PAID 订单数）*/
    Long teamSignOrderCount(@Param("tenantId") Long tenantId,
                            @Param("usernames") List<String> usernames,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);

    /** B6 客单价（团队 + 按成员） */
    Map<String, Object> teamAvgOrder(@Param("tenantId") Long tenantId,
                                     @Param("usernames") List<String> usernames,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    List<Map<String, Object>> memberAvgOrder(@Param("tenantId") Long tenantId,
                                             @Param("usernames") List<String> usernames,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    /** B7 业绩构成（按业务类型） */
    List<Map<String, Object>> performanceCompositionByBusiness(@Param("tenantId") Long tenantId,
                                                               @Param("usernames") List<String> usernames,
                                                               @Param("start") LocalDateTime start,
                                                               @Param("end") LocalDateTime end);

    /** B8 来源转化率（lead.source 维度） */
    List<Map<String, Object>> sourceConversion(@Param("tenantId") Long tenantId,
                                               @Param("usernames") List<String> usernames,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    /** B8 每来源 TOP 转化人 */
    List<Map<String, Object>> sourceTopConverter(@Param("tenantId") Long tenantId,
                                                 @Param("usernames") List<String> usernames,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);

    // ========= 诊断画像 =========

    /** 单成员 6 月业绩 */
    List<Map<String, Object>> memberMonthlyAmountSelf(@Param("tenantId") Long tenantId,
                                                      @Param("username") String username,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    /** 单成员漏斗 */
    Map<String, Object> memberFunnel(@Param("tenantId") Long tenantId,
                                     @Param("username") String username,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    /** 单成员近 7 天每日动作直方图 */
    List<Map<String, Object>> memberDailyActionSelf(@Param("tenantId") Long tenantId,
                                                    @Param("username") String username,
                                                    @Param("from") LocalDateTime from);

    /** 单成员的"即将回收"和"超 N 天"汇总 */
    Map<String, Object> memberRecycleStaleSelf(@Param("tenantId") Long tenantId,
                                               @Param("username") String username,
                                               @Param("leadFrom") Integer leadFrom, @Param("leadTo") Integer leadTo,
                                               @Param("custFrom") Integer custFrom, @Param("custTo") Integer custTo,
                                               @Param("salesDeptUsernames") List<String> salesDeptUsernames);
}
