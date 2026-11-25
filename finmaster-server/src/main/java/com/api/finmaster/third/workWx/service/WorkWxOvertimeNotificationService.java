package com.qykj.finmaster.third.workWx.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 客服群回复超时弹窗推送（WXRESP-12）。
 *
 * <p>参照 {@link com.qykj.finmaster.customer.lead.service.LeadNotificationService} 的做法，用
 * {@link SimpMessagingTemplate#convertAndSendToUser} 向指定客服（username = STOMP principal）推
 * {@code /queue/wecom-overtime-notification}。前端 layout 订阅后用 ElNotification 弹窗（参考现有线索/工单弹窗）。</p>
 */
@Slf4j
@Service
public class WorkWxOvertimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public WorkWxOvertimeNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 向某客服推送"群消息超时未回复"弹窗。
     *
     * @param username       接收客服的登录名（STOMP 用户目的地）
     * @param wecomUserid    该客服的企微 userid（前端"查看明细"直达本人明细用）
     * @param roomId         群 roomid（前端"查看会话"用）
     * @param chatName       群名
     * @param customerUser   最近客户发言人
     * @param customerMsg    客户消息内容预览
     * @param firstCustomerTimeMs 首条客户消息时间(epoch ms，前端定位会话中心点)
     * @param waitedMinutes  已等待分钟（自然时间）
     * @param status         代表回合状态（timeout / unreplied_high_risk 等）
     * @param overdueCount   本次归属此客服的超时未处理回合总数（跨群聚合）
     * @param groupCount     涉及的群数（跨群聚合）
     * @param rounds         本批回合明细（≤50 条，前端"查看明细"弹窗只展示这批，而非全量积压）
     */
    public void notifyOvertime(String username, String wecomUserid, String roomId, String chatName, String customerUser,
                               String customerMsg, Long firstCustomerTimeMs, long waitedMinutes,
                               String status, int overdueCount, int groupCount, List<Map<String, Object>> rounds) {
        if (username == null || username.isEmpty()) {
            return;
        }
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("wecomUserid", wecomUserid);
        dto.put("roomId", roomId);
        dto.put("chatName", chatName);
        dto.put("customerUser", customerUser);
        dto.put("customerMsg", customerMsg);
        dto.put("firstCustomerTimeMs", firstCustomerTimeMs);
        dto.put("waitedMinutes", waitedMinutes);
        dto.put("status", status);
        dto.put("overdueCount", overdueCount);
        dto.put("groupCount", groupCount);
        dto.put("rounds", rounds);
        dto.put("roundsTruncated", rounds != null && overdueCount > rounds.size());
        dto.put("notificationType", "cs_overtime");

        log.debug("[wx-overtime] 推送超时弹窗给客服[{}]：群数={} 未处理={} 最久等待={}分",
                username, groupCount, overdueCount, waitedMinutes);
        try {
            messagingTemplate.convertAndSendToUser(username, "/queue/wecom-overtime-notification", dto);
        } catch (Throwable e) {
            log.warn("[wx-overtime] 推送超时弹窗失败 username={}：{}", username, e.getMessage());
        }
    }
}
