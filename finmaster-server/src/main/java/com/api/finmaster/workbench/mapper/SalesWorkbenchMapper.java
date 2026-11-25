package com.qykj.finmaster.workbench.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 销售工作台专用查询：签单排行、线索/客户聚合、提成业绩
 *
 * 设计：
 *   - 单值方法 (单 username) 用于个人视角；List<String> usernames 版本用于团队视角；
 *   - byMember 方法用于「点击指标弹出人员列表」明细弹窗；
 *   - 同环比统一通过 service 层调三次 (curr / prev / yoy) 计算 growth。
 */
@Mapper
public interface SalesWorkbenchMapper {

    // ============= 签单排行（已存在，不变） =============

    /** 签单数排行：所有 PAID 订单 GROUP BY create_by */
    List<Map<String, Object>> rankSignCount(@Param("tenantId") Long tenantId,
                                            @Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end);

    /** 我的某区间内 PAID 订单数（个人视角） */
    Long countMySignOrders(@Param("tenantId") Long tenantId,
                          @Param("username") String username,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    /** 团队签单数（IN list 版本） */
    Long countTeamSignOrders(@Param("tenantId") Long tenantId,
                             @Param("usernames") List<String> usernames,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    /** 我的某区间内 PAID 订单总金额 */
    BigDecimal sumMySignAmount(@Param("tenantId") Long tenantId,
                              @Param("username") String username,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /** 团队 PAID 订单总金额 */
    BigDecimal sumTeamSignAmount(@Param("tenantId") Long tenantId,
                                 @Param("usernames") List<String> usernames,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end);

    // ============= 线索：个人 + 团队 + byMember =============

    Long countMyLeadCreatedInRange(@Param("tenantId") Long tenantId,
                                   @Param("username") String username,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);
    Long countTeamLeadCreatedInRange(@Param("tenantId") Long tenantId,
                                     @Param("usernames") List<String> usernames,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);

    Long countMyLeadConvertedInRange(@Param("tenantId") Long tenantId,
                                     @Param("username") String username,
                                     @Param("start") LocalDateTime start,
                                     @Param("end") LocalDateTime end);
    Long countTeamLeadConvertedInRange(@Param("tenantId") Long tenantId,
                                       @Param("usernames") List<String> usernames,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);

    Long countMyLeadFollowedInRange(@Param("tenantId") Long tenantId,
                                    @Param("username") String username,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);
    Long countTeamLeadFollowedInRange(@Param("tenantId") Long tenantId,
                                      @Param("usernames") List<String> usernames,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    Long countMyLeadAboutRecycle(@Param("tenantId") Long tenantId,
                                 @Param("username") String username,
                                 @Param("warnFromDays") Integer warnFromDays,
                                 @Param("warnToDays") Integer warnToDays,
                                 @Param("salesDeptUsernames") List<String> salesDeptUsernames);
    Long countTeamLeadAboutRecycle(@Param("tenantId") Long tenantId,
                                   @Param("usernames") List<String> usernames,
                                   @Param("warnFromDays") Integer warnFromDays,
                                   @Param("warnToDays") Integer warnToDays,
                                   @Param("salesDeptUsernames") List<String> salesDeptUsernames);

    // 沿用旧接口（兼容外部）
    Long countMyTotalLeads(@Param("tenantId") Long tenantId, @Param("username") String username);
    Long countMyLeadConvertedAll(@Param("tenantId") Long tenantId, @Param("username") String username);
    Long countMyLeadAll(@Param("tenantId") Long tenantId, @Param("username") String username);
    Long countMyLeadTodayPending(@Param("tenantId") Long tenantId,
                                 @Param("username") String username,
                                 @Param("dayStart") LocalDateTime dayStart,
                                 @Param("dayEnd") LocalDateTime dayEnd);

    /** byMember：每个团队成员在指定区间的 [新增/已转化/已跟进/即将回收] 明细；
     *  salesDeptUsernames 用于"即将回收"子查询与 LeadAutoInvalidateTask 对齐 */
    List<Map<String, Object>> leadStatsByMember(@Param("tenantId") Long tenantId,
                                                @Param("usernames") List<String> usernames,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                @Param("warnFromDays") Integer warnFromDays,
                                                @Param("warnToDays") Integer warnToDays,
                                                @Param("salesDeptUsernames") List<String> salesDeptUsernames);

    // ============= 客户：个人 + 团队 + byMember =============

    Long countMyCustomerTotal(@Param("tenantId") Long tenantId, @Param("username") String username);
    Long countMyCustomerTodayPending(@Param("tenantId") Long tenantId,
                                     @Param("username") String username,
                                     @Param("dayStart") LocalDateTime dayStart,
                                     @Param("dayEnd") LocalDateTime dayEnd);
    Long countMyCustomerDealAll(@Param("tenantId") Long tenantId, @Param("username") String username);

    Long countMyCustomerCreatedInRange(@Param("tenantId") Long tenantId,
                                       @Param("username") String username,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end);
    Long countTeamCustomerCreatedInRange(@Param("tenantId") Long tenantId,
                                         @Param("usernames") List<String> usernames,
                                         @Param("start") LocalDateTime start,
                                         @Param("end") LocalDateTime end);

    Long countMyCustomerDealInRange(@Param("tenantId") Long tenantId,
                                    @Param("username") String username,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);
    Long countTeamCustomerDealInRange(@Param("tenantId") Long tenantId,
                                      @Param("usernames") List<String> usernames,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    Long countMyCustomerFollowedInRange(@Param("tenantId") Long tenantId,
                                        @Param("username") String username,
                                        @Param("start") LocalDateTime start,
                                        @Param("end") LocalDateTime end);
    Long countTeamCustomerFollowedInRange(@Param("tenantId") Long tenantId,
                                          @Param("usernames") List<String> usernames,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    Long countMyCustomerAboutRecycle(@Param("tenantId") Long tenantId,
                                     @Param("username") String username,
                                     @Param("warnFromDays") Integer warnFromDays,
                                     @Param("warnToDays") Integer warnToDays);
    Long countTeamCustomerAboutRecycle(@Param("tenantId") Long tenantId,
                                       @Param("usernames") List<String> usernames,
                                       @Param("warnFromDays") Integer warnFromDays,
                                       @Param("warnToDays") Integer warnToDays);

    /** byMember：客户明细 */
    List<Map<String, Object>> customerStatsByMember(@Param("tenantId") Long tenantId,
                                                    @Param("usernames") List<String> usernames,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end,
                                                    @Param("warnFromDays") Integer warnFromDays,
                                                    @Param("warnToDays") Integer warnToDays);

    /** 工商业务利润（个人 + 团队） */
    Map<String, Object> sumMyBusinessRegProfit(@Param("tenantId") Long tenantId,
                                               @Param("username") String username,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);
    Map<String, Object> sumTeamBusinessRegProfit(@Param("tenantId") Long tenantId,
                                                 @Param("usernames") List<String> usernames,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}
