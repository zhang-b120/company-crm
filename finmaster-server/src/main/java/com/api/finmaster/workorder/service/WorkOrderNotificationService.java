package com.qykj.finmaster.workorder.service;

import com.qykj.finmaster.workorder.domain.WorkOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 任务单 WebSocket 通知
 * 消息路由到 /user/{username}/queue/work-order-notification
 */
@Slf4j
@Service
public class WorkOrderNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public WorkOrderNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 催办推送：发给当前持有人
     */
    public void notifyUrge(WorkOrder wo, String operatorName, String content) {
        String to = wo.getCurrentOwnerUser();
        if (to == null || to.isEmpty()) {
            log.debug("催办推送跳过：任务 {} 无当前持有人", wo.getTaskNo());
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "URGE");
        payload.put("workOrderId", wo.getId());
        payload.put("taskNo", wo.getTaskNo());
        payload.put("title", wo.getTitle());
        payload.put("from", operatorName);
        payload.put("content", content);
        send(to, payload);
    }

    /**
     * 指派通知：发给新持有人（节点流转、子任务创建时可用）
     */
    public void notifyAssign(WorkOrder wo, String toUser) {
        if (toUser == null || toUser.isEmpty()) return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "ASSIGN");
        payload.put("workOrderId", wo.getId());
        payload.put("taskNo", wo.getTaskNo());
        payload.put("title", wo.getTitle());
        payload.put("taskType", wo.getTaskType());
        payload.put("status", wo.getStatus());
        send(toUser, payload);
    }

    /**
     * 任务完成通知：发给发起人（owner_user）
     */
    public void notifyComplete(WorkOrder wo, String operatorName) {
        String to = wo.getOwnerUser();
        if (to == null || to.isEmpty()) {
            log.debug("完成通知跳过：任务 {} 无发起人", wo.getTaskNo());
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "COMPLETE");
        payload.put("workOrderId", wo.getId());
        payload.put("taskNo", wo.getTaskNo());
        payload.put("title", wo.getTitle());
        payload.put("taskType", wo.getTaskType());
        payload.put("status", wo.getStatus());
        payload.put("operatorName", operatorName);
        send(to, payload);
    }

    private void send(String username, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/work-order-notification",
                    payload
            );
            log.debug("任务单通知已送达 {}: {}", username, payload);
        } catch (Exception e) {
            log.warn("任务单通知发送失败 {}: {}", username, e.getMessage());
        }
    }
}
