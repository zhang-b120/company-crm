package com.qykj.finmaster.scheduledTask;

import com.qykj.finmaster.third.workWx.service.WorkWxInvoiceKeywordCollectServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 企业微信"发票业务"关键词命中定时采集（工单89 / WXKW-04）。
 *
 * <p>仅 @Scheduled + 调 service（复杂采集逻辑在 {@link WorkWxInvoiceKeywordCollectServiceImpl}）。
 * 当前为骨架：仅采集打印、不落库。</p>
 */
@Slf4j
@Component
public class WorkWxInvoiceKeywordCollectTask {

    @Autowired
    private WorkWxInvoiceKeywordCollectServiceImpl collectService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void run() {
        try {
            collectService.collectRecent();
        } catch (Throwable e) {
            // 骨架阶段：异常只打日志，不影响其它定时任务
            log.error("[invoice-collect-task] 采集异常", e);
        }
    }
}
