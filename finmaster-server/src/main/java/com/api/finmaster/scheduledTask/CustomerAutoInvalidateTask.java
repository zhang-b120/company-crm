package com.qykj.finmaster.scheduledTask;

import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.customer.customerInfo.service.CustomerInfoServiceImpl;
import com.qykj.finmaster.customer.customerInfoLog.domain.CustomerLog;
import com.qykj.finmaster.customer.customerInfoLog.manager.impl.CustomerLogManagerImpl;
import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户自动回收定时任务
 *
 * 规则（从 sys_biz_config 复合配置 customer.recycle 动态读取）：
 *   前提：私海客户（customerType=1）且尚未签订任何有效订单（paid 且 normal）
 *   触发条件（A 或 B，满足任一即回收）：
 *     A) noFollowDays 天没跟进（lastFollowTime 或 createTime 超阈值）
 *     B) noOrderDays  天没签订单（按 createTime 近似判断）
 *
 *   回收动作：customerType = 2（转入公海）、清空 ownerUser/ownerUserName
 *   dryRun=1：只记录不执行
 *   enabled=0：跳过任务
 */
@Component
@Slf4j
public class CustomerAutoInvalidateTask {

    @Autowired
    private CustomerInfoManagerImpl customerInfoManager;

    @Autowired
    private CustomerInfoServiceImpl customerInfoService;

    @Autowired
    private CustomerLogManagerImpl customerLogManager;

    @Autowired
    private SysBizConfigService bizConfigService;

    /**
     * 每天凌晨 2:30 执行（错开线索回收 2:00）
     */
    @Scheduled(cron = "0 30 2 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoRecycle() {
        Boolean enabled = bizConfigService.getGroupBool("customer.recycle", "enabled", false);
        if (enabled == null || !enabled) {
            log.info("[客户自动回收] 任务未启用，跳过");
            return;
        }
        boolean dryRun = Boolean.TRUE.equals(bizConfigService.getGroupBool("customer.recycle", "dry_run", true));
        int noFollowDays = bizConfigService.getGroupInt("customer.recycle", "no_follow_days", 30);
        int noOrderDays = bizConfigService.getGroupInt("customer.recycle", "no_order_days", 90);

        log.info("[客户自动回收] 任务开始 dryRun={} noFollowDays={} noOrderDays={}",
                dryRun, noFollowDays, noOrderDays);

        Date noFollowThreshold = toDate(LocalDateTime.now().minusDays(noFollowDays));
        Date noOrderThreshold = toDate(LocalDateTime.now().minusDays(noOrderDays));

        // 候选：私海（customerType=1）+ 有 ownerUser + (条件 A 或 条件 B)
        List<CustomerInfo> candidates = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getCustomerType, GlobalConstant.Number.ONE_INT)
                .isNotNull(CustomerInfo::getOwnerUser)
                .ne(CustomerInfo::getOwnerUser, "")
                .and(qw -> qw
                        // 条件 A：超 noFollowDays 没跟进
                        .and(w -> w
                                .and(ww -> ww.isNotNull(CustomerInfo::getLastFollowTime).lt(CustomerInfo::getLastFollowTime, noFollowThreshold))
                                .or(ww -> ww.isNull(CustomerInfo::getLastFollowTime).lt(CustomerInfo::getCreateTime, noFollowThreshold))
                        )
                        // 或 条件 B：超 noOrderDays 没签单
                        .or(w -> w.lt(CustomerInfo::getCreateTime, noOrderThreshold))
                )
                .list();

        log.info("[客户自动回收] 初筛候选 {} 条", candidates.size());
        if (candidates.isEmpty()) return;

        // 前提过滤：剔除已签订有效订单的客户
        Set<Long> transactedIds = findTransactedCustomerIds(candidates);
        if (!transactedIds.isEmpty()) {
            int before = candidates.size();
            candidates.removeIf(c -> transactedIds.contains(c.getId()));
            log.info("[客户自动回收] 剔除已签单客户 {} 条，剩余 {} 条", before - candidates.size(), candidates.size());
        }

        Set<Long> processed = new HashSet<>();
        for (CustomerInfo c : candidates) {
            if (processed.contains(c.getId())) continue;
            processed.add(c.getId());

            String reason = buildRecycleReason(c, noFollowDays, noOrderDays, noFollowThreshold, noOrderThreshold);
            if (dryRun) {
                log.info("[dryRun] 将回收客户 id={} name={} owner={} 原因={}",
                        c.getId(), c.getCustomerName(), c.getOwnerUser(), reason);
            } else {
                customerInfoManager.lambdaUpdate()
                        .set(CustomerInfo::getCustomerType, GlobalConstant.Number.TWO_INT)
                        .set(CustomerInfo::getOwnerUser, null)
                        .set(CustomerInfo::getOwnerUserName, null)
                        .set(CustomerInfo::getUpdateTime, new Date())
                        .eq(CustomerInfo::getId, c.getId())
                        .update();
                writeSystemLog(c, "系统自动回收：" + reason);
            }
        }

        log.info("[客户自动回收] 任务结束 处理 {} 条", processed.size());
    }

    /** 按 tenantId 分组调 CustomerInfoService.getTransactionCustomerIds，聚合出已签单 customerId */
    private Set<Long> findTransactedCustomerIds(List<CustomerInfo> candidates) {
        Map<Long, List<Long>> byTenant = candidates.stream()
                .filter(c -> c.getTenantId() != null)
                .collect(Collectors.groupingBy(
                        CustomerInfo::getTenantId,
                        Collectors.mapping(CustomerInfo::getId, Collectors.toList())
                ));
        Set<Long> result = new HashSet<>();
        byTenant.forEach((tid, ids) -> {
            List<Long> transacted = customerInfoService.getTransactionCustomerIds(ids, tid);
            if (transacted != null) result.addAll(transacted);
        });
        return result;
    }

    private String buildRecycleReason(CustomerInfo c, int noFollowDays, int noOrderDays,
                                      Date noFollowThreshold, Date noOrderThreshold) {
        Date refFollowTime = c.getLastFollowTime() != null ? c.getLastFollowTime() : c.getCreateTime();
        boolean hitNoFollow = refFollowTime != null && refFollowTime.before(noFollowThreshold);
        boolean hitNoOrder = c.getCreateTime() != null && c.getCreateTime().before(noOrderThreshold);

        if (hitNoFollow && hitNoOrder) {
            return String.format("超 %d 天未跟进且超 %d 天未签单", noFollowDays, noOrderDays);
        } else if (hitNoFollow) {
            return String.format("超 %d 天未跟进", noFollowDays);
        } else if (hitNoOrder) {
            return String.format("超 %d 天未签单", noOrderDays);
        }
        return "命中回收规则";
    }

    private void writeSystemLog(CustomerInfo c, String content) {
        try {
            CustomerLog logEntry = new CustomerLog();
            logEntry.setCustomerId(c.getId());
            // logType 5 = 转入公海（对齐 CustomerInfoServiceImpl.transferToPublicSea 的业务语义）
            logEntry.setLogType(GlobalConstant.Number.FIVE_INT);
            logEntry.setContent(content);
            logEntry.setCreateBy("system");
            logEntry.setCreateByName("系统");
            logEntry.setCreateTime(new Date());
            logEntry.setTenantId(c.getTenantId());
            customerLogManager.save(logEntry);
        } catch (Exception e) {
            log.warn("[客户自动回收] 写入日志失败 customerId={}: {}", c.getId(), e.getMessage());
        }
    }

    private static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
