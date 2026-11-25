package com.qykj.finmaster.workbench.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 客服工作台聚合 SQL：基于 work_order 主表 + orders 关联（续费金额）
 */
@Mapper
public interface CsWorkbenchMapper {

    /** 我的某类型任务总数（非终态） */
    Long countMyActiveTasks(@Param("tenantId") Long tenantId,
                            @Param("username") String username,
                            @Param("taskType") String taskType);

    /** 我的某类型任务今日到期数（dueTime ∈ [dayStart, dayEnd) 非终态） */
    Long countMyTodayDue(@Param("tenantId") Long tenantId,
                         @Param("username") String username,
                         @Param("taskType") String taskType,
                         @Param("dayStart") Date dayStart,
                         @Param("dayEnd") Date dayEnd);

    /** 我的某类型超 N 天未动任务数（update_time 距今 ≥ N 天，非终态） */
    Long countMyStaleTasks(@Param("tenantId") Long tenantId,
                           @Param("username") String username,
                           @Param("taskType") String taskType,
                           @Param("staleDays") Integer staleDays);

    /** 我的某类型本月已完成数（status=DONE，complete_time 本月） */
    Long countMyMonthDone(@Param("tenantId") Long tenantId,
                          @Param("username") String username,
                          @Param("taskType") String taskType,
                          @Param("start") Date start,
                          @Param("end") Date end);

    /** 我的某类型本月新增数（create_time 本月） */
    Long countMyMonthNew(@Param("tenantId") Long tenantId,
                         @Param("username") String username,
                         @Param("taskType") String taskType,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    /** 我的续费任务本月到期数（dueTime 在本月） */
    Long countMyMonthDue(@Param("tenantId") Long tenantId,
                         @Param("username") String username,
                         @Param("taskType") String taskType,
                         @Param("start") Date start,
                         @Param("end") Date end);

    /** 我的某类型超期未办数（dueTime < now 且非 DONE/CANCELED） */
    Long countMyOverdue(@Param("tenantId") Long tenantId,
                        @Param("username") String username,
                        @Param("taskType") String taskType,
                        @Param("now") Date now);

    /** 我的续费本月续费金额：work_order(RENEWAL,DONE,complete_time 本月) JOIN orders ON order_id, status=PAID */
    BigDecimal sumMyMonthRenewAmount(@Param("tenantId") Long tenantId,
                                     @Param("username") String username,
                                     @Param("start") Date start,
                                     @Param("end") Date end);

    /**
     * 拉取我的本月已完成任务的 (create_time, complete_time) 对，用于工作时长计算
     * Map keys: createTime / completeTime（LocalDateTime）
     */
    java.util.List<java.util.Map<String, Object>> selectMyMonthCompletedTaskTimes(
            @Param("tenantId") Long tenantId,
            @Param("username") String username,
            @Param("taskType") String taskType,
            @Param("start") Date start,
            @Param("end") Date end);

    /**
     * 本月完成数排行：按 current_owner_user 聚合 status=DONE 的 work_order 数量
     * 关联 sys_user 取 real_name 显示；同租户内全部人员
     * 返回字段：username, real_name, count
     */
    java.util.List<java.util.Map<String, Object>> rankMonthCompletedByOwner(
            @Param("tenantId") Long tenantId,
            @Param("taskType") String taskType,
            @Param("start") Date start,
            @Param("end") Date end);

    /**
     * 按节点排名：本月内指定 step_code 已完成（DONE）的 work_order_step 数量，
     * 按节点持有人 owner_user 聚合倒序。
     * 用于"客户对接"排名仅算"管家填写"(HANDYMAN_FILL)节点的口径
     * 返回字段：username, real_name, count
     */
    java.util.List<java.util.Map<String, Object>> rankMonthCompletedByStep(
            @Param("tenantId") Long tenantId,
            @Param("taskType") String taskType,
            @Param("stepCode") String stepCode,
            @Param("start") Date start,
            @Param("end") Date end);

    /** 我的某类型任务总数（不含 CANCELED / DRAFT，用于完成率分母） */
    Long countMyAllTasks(@Param("tenantId") Long tenantId,
                         @Param("username") String username,
                         @Param("taskType") String taskType);

    /** 我的某类型累计完成数（status=DONE，无时间限制） */
    Long countMyAllDone(@Param("tenantId") Long tenantId,
                        @Param("username") String username,
                        @Param("taskType") String taskType);

    /**
     * 按完成率排名：每人 (DONE 数 / 非 CANCELED/DRAFT 总数 × 100)，按率倒序，相同率按完成数倒序
     * 返回字段：username, real_name, done_count, total_count, rate（BigDecimal 0~100）
     */
    java.util.List<java.util.Map<String, Object>> rankByCompletionRate(
            @Param("tenantId") Long tenantId,
            @Param("taskType") String taskType);

    // ============= 数据收集（按"指定账期"+企业粒度） =============

    /**
     * 我在指定账期下的 DATA_COLLECTION 任务单数（meta_json.$.period = period，非 CANCELED/DRAFT）
     * 用于判断"是否需要展示数据收集模块"
     */
    Long countMyDataCollectionTasksByPeriod(@Param("tenantId") Long tenantId,
                                            @Param("username") String username,
                                            @Param("period") String period);

    /** 指定 butler 管辖的企业总数（company_service 去重 company_id） */
    Long countCompaniesByButler(@Param("tenantId") Long tenantId,
                                @Param("butler") String butler);

    /**
     * 指定 butler 管辖的企业中、对应账期已上传/标记的企业数
     * （company_bank_statement_upload.upload_count > 0 或 no_statement = 1）
     */
    Long countCompaniesUploadedByPeriod(@Param("tenantId") Long tenantId,
                                        @Param("butler") String butler,
                                        @Param("period") String period);

    /**
     * 按"指定账期"的企业完成率排名（参与者：所有 company_service 中出现的 butler）
     * 返回字段：username, real_name, total_count, done_count, rate
     */
    java.util.List<java.util.Map<String, Object>> rankDataCollectionByPeriod(
            @Param("tenantId") Long tenantId,
            @Param("period") String period);

    // ============= 外勤任务（field_work_task 表，用 userId 过滤 assignee_id） =============

    /** 我的外勤任务总数（非 CANCELLED；用于判断模块是否展示） */
    Long countMyFieldWorkAllTasks(@Param("userId") Long userId);

    /** 我的外勤待处理（PENDING / IN_PROGRESS） */
    Long countMyFieldWorkActiveTasks(@Param("userId") Long userId);

    /** 我的外勤本月已完成（COMPLETED 且 actual_end_time 在本月） */
    Long countMyFieldWorkMonthDone(@Param("userId") Long userId,
                                   @Param("start") Date start,
                                   @Param("end") Date end);

    /** 我的外勤超 N 天未完成（PENDING / IN_PROGRESS 且 update_time 距今 ≥N） */
    Long countMyFieldWorkStaleTasks(@Param("userId") Long userId,
                                    @Param("staleDays") Integer staleDays);

    /** 我的外勤本月新增（create_time 在本月） */
    Long countMyFieldWorkMonthNew(@Param("userId") Long userId,
                                  @Param("start") Date start,
                                  @Param("end") Date end);

    /** 我的外勤本月已完成的 (create_time, actual_end_time) 对，用于平均工作时长计算 */
    java.util.List<java.util.Map<String, Object>> selectMyFieldWorkMonthCompletedTimes(
            @Param("userId") Long userId,
            @Param("start") Date start,
            @Param("end") Date end);

    /** 外勤本月完成数排名（按 assignee_id 聚合，关联 sys_user 取 real_name） */
    java.util.List<java.util.Map<String, Object>> rankFieldWorkMonthCompleted(
            @Param("tenantId") Long tenantId,
            @Param("start") Date start,
            @Param("end") Date end);

    // ============= 团队（IN list）变体 =============

    /** 团队任务总数（非终态） */
    Long countTeamActiveTasks(@Param("tenantId") Long tenantId,
                              @Param("usernames") java.util.List<String> usernames,
                              @Param("taskType") String taskType);

    /** 团队某区间已完成数 */
    Long countTeamMonthDone(@Param("tenantId") Long tenantId,
                            @Param("usernames") java.util.List<String> usernames,
                            @Param("taskType") String taskType,
                            @Param("start") Date start,
                            @Param("end") Date end);

    /** 团队某区间新增数 */
    Long countTeamMonthNew(@Param("tenantId") Long tenantId,
                           @Param("usernames") java.util.List<String> usernames,
                           @Param("taskType") String taskType,
                           @Param("start") java.time.LocalDateTime start,
                           @Param("end") java.time.LocalDateTime end);

    /** 团队超 N 天未动数 */
    Long countTeamStaleTasks(@Param("tenantId") Long tenantId,
                             @Param("usernames") java.util.List<String> usernames,
                             @Param("taskType") String taskType,
                             @Param("staleDays") Integer staleDays);

    /** 团队某区间到期数（dueTime 在区间） */
    Long countTeamMonthDue(@Param("tenantId") Long tenantId,
                           @Param("usernames") java.util.List<String> usernames,
                           @Param("taskType") String taskType,
                           @Param("start") Date start,
                           @Param("end") Date end);

    /** 团队超期未办（dueTime < now 非 DONE/CANCELED） */
    Long countTeamOverdue(@Param("tenantId") Long tenantId,
                          @Param("usernames") java.util.List<String> usernames,
                          @Param("taskType") String taskType,
                          @Param("now") Date now);

    /** 团队某区间续费金额（PAID JOIN） */
    BigDecimal sumTeamMonthRenewAmount(@Param("tenantId") Long tenantId,
                                       @Param("usernames") java.util.List<String> usernames,
                                       @Param("start") Date start,
                                       @Param("end") Date end);

    /** 团队本月已完成 (create_time, complete_time) 对，用于平均工作时长 */
    java.util.List<java.util.Map<String, Object>> selectTeamMonthCompletedTaskTimes(
            @Param("tenantId") Long tenantId,
            @Param("usernames") java.util.List<String> usernames,
            @Param("taskType") String taskType,
            @Param("start") Date start,
            @Param("end") Date end);

    /** 团队代账任务 byMember：每人 [active/monthDone/stale/monthNew/avgHours] */
    java.util.List<java.util.Map<String, Object>> bookkeepingStatsByMember(
            @Param("tenantId") Long tenantId,
            @Param("usernames") java.util.List<String> usernames,
            @Param("monthStart") Date monthStart,
            @Param("monthEnd") Date monthEnd,
            @Param("staleDays") Integer staleDays);

    /** 团队续费任务 byMember */
    java.util.List<java.util.Map<String, Object>> renewalStatsByMember(
            @Param("tenantId") Long tenantId,
            @Param("usernames") java.util.List<String> usernames,
            @Param("monthStart") Date monthStart,
            @Param("monthEnd") Date monthEnd,
            @Param("now") Date now);

    /** 团队数据收集 byMember（按指定账期） */
    java.util.List<java.util.Map<String, Object>> dataCollectionStatsByMember(
            @Param("tenantId") Long tenantId,
            @Param("usernames") java.util.List<String> usernames,
            @Param("period") String period);

    /** 团队外勤 byMember（按 userId 列表） */
    java.util.List<java.util.Map<String, Object>> fieldWorkStatsByMember(
            @Param("tenantId") Long tenantId,
            @Param("userIds") java.util.List<Long> userIds,
            @Param("monthStart") Date monthStart,
            @Param("monthEnd") Date monthEnd,
            @Param("staleDays") Integer staleDays);

    /** 团队外勤总任务（用于判断模块是否展示） */
    Long countTeamFieldWorkAll(@Param("userIds") java.util.List<Long> userIds);

    /** 团队外勤待处理 */
    Long countTeamFieldWorkActive(@Param("userIds") java.util.List<Long> userIds);

    /** 团队外勤本月已完成 */
    Long countTeamFieldWorkMonthDone(@Param("userIds") java.util.List<Long> userIds,
                                     @Param("start") Date start,
                                     @Param("end") Date end);

    /** 团队外勤超 N 天未完成 */
    Long countTeamFieldWorkStale(@Param("userIds") java.util.List<Long> userIds,
                                 @Param("staleDays") Integer staleDays);

    /** 团队外勤本月新增 */
    Long countTeamFieldWorkMonthNew(@Param("userIds") java.util.List<Long> userIds,
                                    @Param("start") Date start,
                                    @Param("end") Date end);

    /** 团队外勤本月已完成 (create_time, actual_end_time) 对 */
    java.util.List<java.util.Map<String, Object>> selectTeamFieldWorkMonthCompletedTimes(
            @Param("userIds") java.util.List<Long> userIds,
            @Param("start") Date start,
            @Param("end") Date end);
}
