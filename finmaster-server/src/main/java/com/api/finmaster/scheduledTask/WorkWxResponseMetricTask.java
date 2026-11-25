package com.qykj.finmaster.scheduledTask;

import com.qykj.finmaster.third.workWx.service.WorkWxResponseMetricServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * 企业微信群回复/超时指标增量重算定时任务（工单 76 / WXRESP-01，含 2026-06-20 窗口修正）。
 *
 * <p><b>必须用"滚动回看窗口"而非"上次水位之后的紧增量"：</b>recompute 会从窗口起点重切回合，
 * 若同一回合的客户消息落在窗口外、员工回复落在窗口内，回复会被当作主动发言忽略 → 该回合误报为
 * pending/unreplied_high_risk。固定每轮回看 {@link #LOOKBACK_DAYS} 天（≥ 会话存档约 5–7 天保留窗口），
 * 使同一回合的客户消息与回复几乎总落在同一窗口内 → 迟到回复时同一唯一键行自愈翻转为 closed；
 * 超窗的老回合沉淀为历史，不再变动。<b>切勿改成紧增量。</b></p>
 */
@Slf4j
@Component
public class WorkWxResponseMetricTask {

    /** 每轮回看天数（≥ 会话存档保留窗口，保证跨窗回合自愈） */
    private static final int LOOKBACK_DAYS = 7;

    @Autowired
    private WorkWxResponseMetricServiceImpl responseService;

    @Scheduled(cron = "0 */5 * * * ?")
    public void run() {
        try {
            // 滚动回看 now-7天（非"上次水位之后"），保证同一回合客户消息与回复落同一窗口、自愈为 closed
            Date since = new Date(System.currentTimeMillis() - LOOKBACK_DAYS * 24L * 3600 * 1000);
            Map<String, Object> r = responseService.recompute(since);
            log.info("[wxresp-task] 回复指标重算完成：{}", r);
        } catch (Throwable e) {
            log.error("[wxresp-task] 回复指标重算异常", e);
        }
    }
}
