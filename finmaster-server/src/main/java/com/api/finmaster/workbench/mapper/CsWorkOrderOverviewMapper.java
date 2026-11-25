package com.qykj.finmaster.workbench.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 客服部工单总览聚合 SQL（只读 work_order）。工单 74 / CSWO-01。
 * SQL 范式照搬 {@link CsWorkbenchMapper#bookkeepingStatsByMember}（SUM(CASE) 按 COALESCE(current_owner_user,owner_user) 分组）。
 * 超时口径：due_time < now AND status 非终态（DONE/CANCELED/REJECTED）。
 */
@Mapper
public interface CsWorkOrderOverviewMapper {

    /**
     * 按客服人员的工单状态分布 + 本月完成 + 超时 + 停滞。
     * 返回字段：username, real_name, pending, running, waiting_child, suspended, concession_done,
     *           waiting_fix, active_count, month_done, overdue_pending, overdue_running, stale_count
     */
    List<Map<String, Object>> overviewByMember(@Param("tenantId") Long tenantId,
                                               @Param("usernames") List<String> usernames,
                                               @Param("taskType") String taskType,
                                               @Param("now") Date now,
                                               @Param("monthStart") Date monthStart,
                                               @Param("monthEnd") Date monthEnd,
                                               @Param("createStart") Date createStart,
                                               @Param("createEnd") Date createEnd,
                                               @Param("staleDays") Integer staleDays);

    /**
     * 指定状态的超时明细（PENDING / RUNNING），按到期时间升序，limit 条。
     * 返回字段：id, task_no, title, current_owner_user, current_owner_name, due_time, overdue_days
     */
    List<Map<String, Object>> overdueDetail(@Param("tenantId") Long tenantId,
                                            @Param("usernames") List<String> usernames,
                                            @Param("taskType") String taskType,
                                            @Param("status") String status,
                                            @Param("now") Date now,
                                            @Param("createStart") Date createStart,
                                            @Param("createEnd") Date createEnd,
                                            @Param("limit") Integer limit);

    /** 挂起原因汇总：status='SUSPENDED' GROUP BY suspend_reason。返回 reason, cnt。 */
    List<Map<String, Object>> suspendReasonSummary(@Param("tenantId") Long tenantId,
                                                   @Param("usernames") List<String> usernames,
                                                   @Param("taskType") String taskType,
                                                   @Param("createStart") Date createStart,
                                                   @Param("createEnd") Date createEnd);

    /**
     * 挂起单明细。返回 id, task_no, title, current_owner_user, current_owner_name,
     * suspend_reason, suspend_remark, planned_resume_time, resume_overdue(planned_resume_time<now ? 1 : 0)
     */
    List<Map<String, Object>> suspendDetail(@Param("tenantId") Long tenantId,
                                            @Param("usernames") List<String> usernames,
                                            @Param("taskType") String taskType,
                                            @Param("now") Date now,
                                            @Param("createStart") Date createStart,
                                            @Param("createEnd") Date createEnd);

    /**
     * 下钻明细列表（工单 74 / CSWO-03）：按 bucket 过滤客服部持有工单，分页。
     * username 非空仅看该人；bucket 见 service 注释；ORDER BY create_time DESC。
     * 返回 task_no/task_type/title/current_owner_user/current_owner_name/status/due_time/create_time/company_id/customer_id 及 id。
     */
    List<Map<String, Object>> drillList(@Param("tenantId") Long tenantId,
                                        @Param("usernames") List<String> usernames,
                                        @Param("username") String username,
                                        @Param("bucket") String bucket,
                                        @Param("taskType") String taskType,
                                        @Param("now") Date now,
                                        @Param("monthStart") Date monthStart,
                                        @Param("monthEnd") Date monthEnd,
                                        @Param("createStart") Date createStart,
                                        @Param("createEnd") Date createEnd,
                                        @Param("staleDays") Integer staleDays,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    /** 下钻明细总数（与 drillList 同 WHERE）。 */
    Long drillCount(@Param("tenantId") Long tenantId,
                    @Param("usernames") List<String> usernames,
                    @Param("username") String username,
                    @Param("bucket") String bucket,
                    @Param("taskType") String taskType,
                    @Param("now") Date now,
                    @Param("monthStart") Date monthStart,
                    @Param("monthEnd") Date monthEnd,
                    @Param("createStart") Date createStart,
                    @Param("createEnd") Date createEnd,
                    @Param("staleDays") Integer staleDays);
}
