package com.qykj.finmaster.sale.fieldwork.service;

import com.qykj.finmaster.sale.fieldwork.domain.dto.FieldWorkProgressNotifyDTO;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkProgressRecord;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkTask;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.manager.UserManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Slf4j
@Service
public class FieldWorkNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserManager userManager;

    public FieldWorkNotificationService(SimpMessagingTemplate messagingTemplate,
                                        UserManager userManager) {
        this.messagingTemplate = messagingTemplate;
        this.userManager = userManager;
    }

    /**
     * 提交过程记录后，通知任务下发人
     */
    public void notifyDispatcher(FieldWorkTask task, FieldWorkProgressRecord record) {
        if (task.getDispatcherId() == null) {
            return;
        }

        // 通过下发人ID查找用户名（用于 WebSocket 路由）
        User dispatcher = userManager.getById(task.getDispatcherId());
        if (dispatcher == null || dispatcher.getUsername() == null) {
            log.warn("外勤过程通知：找不到下发人 userId={}", task.getDispatcherId());
            return;
        }

        FieldWorkProgressNotifyDTO dto = new FieldWorkProgressNotifyDTO();
        dto.setTaskId(task.getId());
        dto.setTaskNo(task.getTaskNo());
        dto.setCustomerName(task.getCustomerName());
        dto.setSubmitterName(record.getSubmitterName());
        dto.setProgressId(record.getId());

        String content = record.getContent();
        dto.setContentPreview(content != null && content.length() > 80
                ? content.substring(0, 80) + "..."
                : content);

        dto.setSubmitTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(record.getSubmitTime()));

        log.debug("发送外勤过程通知给下发人 [{}]: taskId={}", dispatcher.getUsername(), task.getId());

        messagingTemplate.convertAndSendToUser(
                dispatcher.getUsername(),
                "/queue/field-work-notification",
                dto
        );
    }
}
