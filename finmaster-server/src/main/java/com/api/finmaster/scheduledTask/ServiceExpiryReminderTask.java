package com.qykj.finmaster.scheduledTask;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.customer.company.domain.CompanyService;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.customer.company.manager.impl.CompanyServiceManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 公司服务到期提醒定时任务（S2 续费闭环 6.3.2）
 *
 * 规则：
 *   每日早上 9:00 执行
 *   扫描 actual_end_date 在 ≤ 30 天内到期的合作中公司
 *   分三档分级：≤7天 红色高危 / 8~15天 橙色 / 16~30天 黄色
 *   按 butler_owner_user 分组，给每位管家推 WS 通知（汇总条数 + 最紧急 5 条）
 *
 * WS 通道：/user/queue/renewal-reminder
 *   消息格式：{ urgentCount, warmCount, normalCount, urgentList: [...] }
 */
@Component
@Slf4j
public class ServiceExpiryReminderTask {

    @Autowired
    private CompanyServiceManagerImpl companyServiceManager;

    @Autowired
    private CustomerCompanyManager customerCompanyManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Value("${renewal.reminder.enabled:true}")
    private boolean enabled;

    /** 每日早上 9 点执行 */
    @Scheduled(cron = "0 0 9 * * ?")
    public void scanExpiringServices() {
        if (!enabled) {
            log.info("[续费提醒] 任务已禁用，跳过");
            return;
        }
        log.info("[续费提醒] 任务开始");

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(30);

        // 查所有 actual_end_date 在 [今天-7, 30天后] 内的服务（提前预警 + 已过期短期内）
        List<CompanyService> services = companyServiceManager.lambdaQuery()
                .isNotNull(CompanyService::getActualEndDate)
                .ge(CompanyService::getActualEndDate, today.minusDays(7))
                .le(CompanyService::getActualEndDate, cutoff)
                .list();

        if (CollUtil.isEmpty(services)) {
            log.info("[续费提醒] 无待提醒服务");
            return;
        }

        // 仅保留合作中公司
        List<Long> companyIds = services.stream()
                .map(CompanyService::getCompanyId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, CustomerCompany> coopCompanies = customerCompanyManager.lambdaQuery()
                .in(CustomerCompany::getId, companyIds)
                .eq(CustomerCompany::getCompanyStatus, "COOPERATING")
                .list().stream()
                .collect(Collectors.toMap(CustomerCompany::getId, c -> c, (a, b) -> a));

        // 按 butlerOwnerUser 分组
        Map<String, List<CompanyService>> byOwner = new HashMap<>();
        for (CompanyService s : services) {
            if (!coopCompanies.containsKey(s.getCompanyId())) continue;
            String owner = s.getButlerOwnerUser();
            if (owner == null || owner.isEmpty()) continue;
            byOwner.computeIfAbsent(owner, k -> new java.util.ArrayList<>()).add(s);
        }

        if (byOwner.isEmpty()) {
            log.info("[续费提醒] 无可推送对象（管家未分配）");
            return;
        }

        log.info("[续费提醒] 推送对象 {} 人", byOwner.size());
        for (Map.Entry<String, List<CompanyService>> entry : byOwner.entrySet()) {
            try {
                pushReminderToOwner(entry.getKey(), entry.getValue(), today, coopCompanies);
            } catch (Exception e) {
                log.warn("[续费提醒] 推送失败 owner={}: {}", entry.getKey(), e.getMessage());
            }
        }
        log.info("[续费提醒] 任务结束");
    }

    private void pushReminderToOwner(String owner, List<CompanyService> services, LocalDate today, Map<Long, CustomerCompany> companies) {
        int urgent = 0, warm = 0, normal = 0;
        List<Map<String, Object>> urgentList = new java.util.ArrayList<>();
        for (CompanyService s : services) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(today, s.getActualEndDate());
            if (days <= 7) urgent++;
            else if (days <= 15) warm++;
            else normal++;
            // 收集最紧急的 5 条作为详情展示
            if (urgentList.size() < 5 && days <= 15) {
                CustomerCompany c = companies.get(s.getCompanyId());
                Map<String, Object> item = new HashMap<>();
                item.put("companyId", s.getCompanyId());
                item.put("companyName", c != null ? c.getCompanyName() : "");
                item.put("daysLeft", days);
                item.put("actualEndDate", s.getActualEndDate());
                urgentList.add(item);
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("urgentCount", urgent);
        payload.put("warmCount", warm);
        payload.put("normalCount", normal);
        payload.put("totalCount", urgent + warm + normal);
        payload.put("urgentList", urgentList);
        payload.put("notificationType", "renewal_reminder");

        log.info("[续费提醒] 推送给 {} —— 紧急 {}, 中度 {}, 正常 {}", owner, urgent, warm, normal);
        messagingTemplate.convertAndSendToUser(owner, "/queue/renewal-reminder", payload);
    }
}
