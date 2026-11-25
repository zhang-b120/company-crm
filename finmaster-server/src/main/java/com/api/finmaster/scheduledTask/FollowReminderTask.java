package com.qykj.finmaster.scheduledTask;

import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.LeadManager;
import com.qykj.finmaster.customer.lead.service.LeadNotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 线索跟进到期提醒定时任务
 *
 * 规则：
 *   每 15 分钟扫一次，查 next_follow_time 在"当前时刻 ± 30 分钟"窗口内、尚未提醒过的线索
 *   推送 WS 通知给 owner_user（前端 /queue/lead-notification 已有订阅）
 *
 * 去重：
 *   使用内存 Set 记录"今日已提醒的线索 id"，凌晨自动清空。
 *   这避免每 15 分钟重复推同一条线索的问题。
 *   **限制**：服务重启后内存丢失 → 重复提醒，可接受
 *   **限制**：多实例部署时会重复推送 → 单机暂可接受，多机建议换 Redis
 */
@Component
@Slf4j
public class FollowReminderTask {

    @Autowired
    private LeadManager leadManager;

    @Autowired
    private LeadNotificationService leadNotificationService;

    /** 今日已提醒的线索 ID（午夜清零） */
    private final Set<Long> remindedToday = new HashSet<>();

    /** 每 15 分钟扫一次 */
    @Scheduled(cron = "0 */15 * * * ?")
    public void scanAndRemind() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(15);
        LocalDateTime windowEnd = now.plusMinutes(30);
        Date windowStartDate = toDate(windowStart);
        Date windowEndDate = toDate(windowEnd);

        // 候选：未转化（status IN 1,2），有 next_follow_time 且在窗口内，有 owner_user
        List<Lead> candidates = leadManager.lambdaQuery()
                .in(Lead::getStatus, GlobalConstant.Number.ONE_INT, GlobalConstant.Number.TWO_INT)
                .isNotNull(Lead::getNextFollowTime)
                .ge(Lead::getNextFollowTime, windowStart)
                .le(Lead::getNextFollowTime, windowEnd)
                .isNotNull(Lead::getOwnerUser)
                .ne(Lead::getOwnerUser, "")
                .list();

        if (candidates.isEmpty()) return;

        log.info("[跟进提醒] 窗口 [{} ~ {}] 候选 {} 条", windowStart, windowEnd, candidates.size());

        int pushed = 0;
        for (Lead lead : candidates) {
            if (remindedToday.contains(lead.getId())) {
                continue;
            }
            try {
                leadNotificationService.notifyFollowReminder(lead);
                remindedToday.add(lead.getId());
                pushed++;
            } catch (Exception e) {
                log.warn("[跟进提醒] 推送失败 leadId={}: {}", lead.getId(), e.getMessage());
            }
        }
        log.info("[跟进提醒] 本轮推送 {} 条", pushed);
    }

    /** 每天凌晨 0:10 清空"今日已提醒"集合 */
    @Scheduled(cron = "0 10 0 * * ?")
    public void resetDailyCache() {
        log.info("[跟进提醒] 清空今日已提醒缓存，上一日累计 {} 条", remindedToday.size());
        remindedToday.clear();
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
