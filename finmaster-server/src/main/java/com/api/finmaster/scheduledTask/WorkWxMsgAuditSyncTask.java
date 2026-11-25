package com.qykj.finmaster.scheduledTask;

import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import com.qykj.finmaster.third.workWx.service.WorkWxMsgAuditSyncServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 企业微信会话存档增量同步定时任务（工单 75 / WXMSG-02；工单 83 / WXSYNC-01 加同步模式开关）。
 *
 * <p>每 2 分钟一轮——会话存档在企微侧仅约 5 天可拉取窗口、过期不可补拉，故需高频定时拉走。
 * 物料（SDK + secret + RSA 私钥）未就位时探测后直接返回，空转不报错。</p>
 *
 * <p>同步模式 {@code wecom.msgaudit.sync_mode}：POLL=本轮询触发（默认）；CALLBACK=改由 msgaudit_notify 回调触发，
 * 本任务默认跳过，仅按 {@code callback_safety_poll_minutes} 兜底轮询，防回调静默丢消息。读配置异常缺省按 POLL。</p>
 */
@Slf4j
@Component
public class WorkWxMsgAuditSyncTask {

    private static final String CFG_SYNC_MODE = "wecom.msgaudit.sync_mode";
    private static final String CFG_SAFETY_POLL_MIN = "wecom.msgaudit.callback_safety_poll_minutes";
    private static final String MODE_POLL = "POLL";
    private static final String MODE_CALLBACK = "CALLBACK";

    @Autowired
    private WorkWxMsgAuditSyncServiceImpl syncService;
    @Autowired
    private SysBizConfigService bizConfigService;

    @Scheduled(cron = "0 */2 * * * ?")
    public void run() {
        if (!syncService.materialsReady()) {
            // 物料未就位：空转，不报错（debug 级别避免刷屏）
            log.debug("[msgaudit-sync-task] 会话存档物料未就位，跳过本轮");
            return;
        }
        String mode = resolveMode();
        if (!MODE_POLL.equalsIgnoreCase(mode)) {
            // CALLBACK 模式：默认由回调触发，本任务跳过；仅当距上次"真正同步"超过安全间隔时兜底轮询一次。
            // WXSYNC-01：用 syncService.getLastRunMs()（回调/轮询/手动任一路真同步都刷新），不再用本地戳——
            // 否则回调一直在同步、本任务仍每 N 分钟空跑兜底。
            int safetyMin = safetyPollMinutes();
            long now = System.currentTimeMillis();
            if (safetyMin <= 0 || now - syncService.getLastRunMs() < safetyMin * 60_000L) {
                log.debug("[msgaudit-sync-task] 同步模式={}，本轮跳过（由回调触发）", mode);
                return;
            }
            log.info("[msgaudit-sync-task] 同步模式={}，距上次同步超过 {} 分钟，执行安全兜底轮询", mode, safetyMin);
        }
        doSync();
    }

    private void doSync() {
        try {
            Map<String, Object> r = syncService.syncIncremental();
            log.info("[msgaudit-sync-task] 增量同步完成：{}", r);
        } catch (Throwable e) {
            log.error("[msgaudit-sync-task] 增量同步异常", e);
        }
    }

    /** 读同步模式；任何异常缺省按 POLL（保证默认轮询不被配置故障打断）。 */
    private String resolveMode() {
        try {
            return bizConfigService.getString(CFG_SYNC_MODE, MODE_POLL);
        } catch (Throwable e) {
            log.warn("[msgaudit-sync-task] 读取同步模式失败，按默认 POLL：{}", e.getMessage());
            return MODE_POLL;
        }
    }

    private int safetyPollMinutes() {
        try {
            Integer v = bizConfigService.getInt(CFG_SAFETY_POLL_MIN, 30);
            return v != null ? v : 30;
        } catch (Throwable e) {
            return 30;
        }
    }
}
