package com.qykj.finmaster.scheduledTask;

import com.qykj.finmaster.third.workWx.service.WorkWxKeywordScanServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 企业微信关键词增量扫描定时任务（工单 77 / WXKW-01）。每 5 分钟扫一批未扫消息。
 */
@Slf4j
@Component
public class WorkWxKeywordScanTask {

    @Autowired
    private WorkWxKeywordScanServiceImpl scanService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void run() {
        try {
            Map<String, Object> r = scanService.scanNew();
            // 无消息时 scanned=0，静默
            if (r != null && Long.parseLong(String.valueOf(r.getOrDefault("scanned", 0))) > 0) {
                log.info("[wxkw-task] 关键词扫描：{}", r);
            }
        } catch (Throwable e) {
            log.error("[wxkw-task] 关键词扫描异常", e);
        }
    }
}
