package com.qykj.finmaster.scheduledTask;

import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.customer.lead.constants.LeadRecycleConstants;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.LeadManager;
import com.qykj.finmaster.customer.lead.mapper.LeadMapper;
import com.qykj.finmaster.customer.leadLog.entity.LeadLog;
import com.qykj.finmaster.customer.leadLog.manager.LeadLogManager;
import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 线索自动回收定时任务
 *
 * 复用 {@link LeadMapper#getOutLeadList} SQL 一次性完成所有过滤（含已转私海 / 已签单 / 已开票 / 销售部人员）。
 * 不在 Java 端做二次循环过滤。
 *
 * 配置（sys_biz_config.lead.recycle 复合配置 + 固定常量）：
 *   - enabled        ：开关
 *   - dry_run        ：灰度模式（只 log 不动数据）
 *   - no_follow_days ：N 天没跟进自动回收
 *   - no_order_days  ：N 天没签单自动回收
 *   - SALES_DEPT_ID  ：销售部部门 ID（含子部门成员，写死=6）
 *
 * 回收动作 = 清空 owner_user / owner_user_name；并向 lead_log 写一条 type=3 的"系统回收"记录。
 */
@Component
@Slf4j
public class LeadAutoInvalidateTask {

    @Autowired
    private LeadManager leadManager;

    @Autowired
    private LeadMapper leadMapper;

    @Autowired
    private LeadLogManager leadLogManager;

    @Autowired
    private SysBizConfigService bizConfigService;

    @Autowired
    private UserMapper userMapper;

    /** 销售部部门 ID：仅回收该部门及其子部门下成员名下的线索（共用 {@link LeadRecycleConstants#SALES_DEPT_ID}） */
    private static final Long SALES_DEPT_ID = LeadRecycleConstants.SALES_DEPT_ID;

    /**
     * 每天凌晨 2:00 执行
     */
    //@Scheduled(cron = "0 0 2 * * ?")
    @Scheduled(cron = "0 35 17 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoRecycle() {
        System.out.println("线索回收开始");
        // ===== Step 1：读配置 =====
        Boolean enabled = bizConfigService.getGroupBool("lead.recycle", "enabled", true);
        if (enabled == null || !enabled) {
            log.info("[线索自动回收] 任务未启用，跳过");
            return;
        }
        boolean dryRun = Boolean.TRUE.equals(bizConfigService.getGroupBool("lead.recycle", "dry_run", true));
        int noFollowDays = bizConfigService.getGroupInt("lead.recycle", "no_follow_days", 14);
        int noOrderDays = bizConfigService.getGroupInt("lead.recycle", "no_order_days", 60);

        // ===== Step 2：取销售部（含子部门）成员 username 列表；查不到则跳过任务 =====
        List<String> userNameList = userMapper.selectUsersByDeptWithChildren(SALES_DEPT_ID, null).stream()
                .map(User::getUsername)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (userNameList.isEmpty()) {
            log.warn("[线索自动回收] 销售部（deptId={}）下无用户，跳过本次回收", SALES_DEPT_ID);
            return;
        }

        // ===== Step 3：拼参数，复用 LeadMapper.getOutLeadList SQL（所有过滤都在 SQL 内完成） =====
        Date noFollowDate = toDate(LocalDateTime.now().minusDays(noFollowDays));
        Date noOrderDate  = toDate(LocalDateTime.now().minusDays(noOrderDays));
        Map<String, Object> params = new HashMap<>();
        params.put("tenantId",     resolveTenantId());
        params.put("noFollowDate", noFollowDate);
        params.put("noOrderDate",  noOrderDate);
        params.put("userNameList", userNameList);

        log.info("[线索自动回收] 任务开始 dryRun={} noFollowDays={} noOrderDays={} 销售部成员={} 人",
                dryRun, noFollowDays, noOrderDays, userNameList.size());

        // ===== Step 4：单次 SQL 查询，结果即"需要回收"的全量候选 =====
        List<Lead> toRecycle = leadMapper.getOutLeadList(params);
        log.info("[线索自动回收] 命中 {} 条", toRecycle.size());

        // ===== Step 5：逐条回收（dryRun 时仅日志） =====
        Date now = new Date();
        int recycled = 0;
        for (Lead lead : toRecycle) {
            String reason = String.format("超 %d 天未跟进 或 超 %d 天未签单", noFollowDays, noOrderDays);
            int nextReleaseCount = (lead.getReleaseCount() == null ? 0 : lead.getReleaseCount()) + 1;
            if (dryRun) {

            } else {
                leadManager.lambdaUpdate()
                        // 清归属人，回到"待分配"池
                        .set(Lead::getOwnerUser, null)
                        .set(Lead::getOwnerUserName, null)
                        // 更新跟进信息（用户要求每次释放也算一次系统跟进时间戳）
                        .set(Lead::getLastFollowContent, reason)
                        .set(Lead::getLastFollowTime, now)
                        // 释放标记：次数 +1、记录本次释放时间
                        .set(Lead::getReleaseCount, nextReleaseCount)
                        .set(Lead::getLastReleaseTime, now)
                        .set(Lead::getUpdateTime, now)
                        .eq(Lead::getId, lead.getId())
                        .update();
                writeSystemLog(lead.getId(),
                        String.format("系统自动回收（第 %d 次）：%s", nextReleaseCount, reason), 3);
            }
            recycled++;
        }
        log.info("[线索自动回收] 任务结束 实际回收={}",  recycled);
    }

    /** 取当前任务的 tenantId：定时任务无登录上下文，按 DB 单租户场景默认 1L；多租户场景需另外注入 */
    private Long resolveTenantId() {
        return 1L;
    }

    private void writeSystemLog(Long leadId, String content, Integer logType) {
        try {
            LeadLog log = new LeadLog();
            log.setLeadId(leadId);
            log.setLogType(logType);
            log.setContent(content);
            log.setOperatorName("系统");
            log.setCreateBy("system");
            log.setCreateByName("系统");
            log.setCreateTime(new Date());
            leadLogManager.save(log);
        } catch (Exception e) {
            LeadAutoInvalidateTask.log.warn("[线索自动回收] 写入系统日志失败 leadId={}: {}", leadId, e.getMessage());
        }
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
