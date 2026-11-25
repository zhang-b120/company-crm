package com.qykj.finmaster.customer.lead.service;

import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.entity.resp.LeadNotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LeadNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public LeadNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * 通知线索归属人
     *
     * @param lead             线索实体
     * @param notificationType 通知类型: "new" 新建 / "assign" 分配 / "follow_reminder" 跟进提醒
     */
    public void notifyOwner(Lead lead, String notificationType) {
        if (lead.getOwnerUser() == null || lead.getOwnerUser().isEmpty()) {
            return;
        }

        LeadNotificationDTO dto = new LeadNotificationDTO();
        dto.setLeadId(lead.getId());
        dto.setLeadName(lead.getLeadName());
        dto.setContactName(lead.getContactName());
        dto.setPhone(lead.getPhone());
        dto.setSource(lead.getSource());
        dto.setCreateTime(lead.getCreateTime());
        dto.setNotificationType(notificationType);

        log.debug("发送线索通知给用户 [{}]: leadId={}, type={}",
                lead.getOwnerUser(), lead.getId(), notificationType);

        messagingTemplate.convertAndSendToUser(
                lead.getOwnerUser(),
                "/queue/lead-notification",
                dto
        );
    }

    /**
     * 跟进提醒：给指定用户推送"该跟进了"通知
     * 供 FollowReminderTask 调用
     */
    public void notifyFollowReminder(Lead lead) {
        notifyOwner(lead, "follow_reminder");
    }

    /**
     * 小程序首次注册通知：用户首次调用 uaAdd 时，提示该线索的负责人
     *  "用户 XXX 已成功注册小程序"。
     *
     * @param lead           被关联到的线索（必须含 ownerUser，否则跳过）
     * @param userIdentifier 用于展示的用户标识（一般为 phone，必要时退化为 contactName）
     */
    public void notifyAppletRegister(Lead lead, String userIdentifier) {
        if (lead == null || lead.getOwnerUser() == null || lead.getOwnerUser().isEmpty()) {
            return;
        }
        LeadNotificationDTO dto = new LeadNotificationDTO();
        dto.setLeadId(lead.getId());
        dto.setLeadName(lead.getLeadName());
        dto.setContactName(userIdentifier != null ? userIdentifier : lead.getContactName());
        dto.setPhone(lead.getPhone());
        dto.setSource(lead.getSource());
        dto.setCreateTime(lead.getCreateTime());
        dto.setNotificationType("applet_register");

        log.debug("发送小程序注册通知给用户 [{}]: leadId={}, identifier={}",
                lead.getOwnerUser(), lead.getId(), userIdentifier);

        messagingTemplate.convertAndSendToUser(
                lead.getOwnerUser(),
                "/queue/lead-notification",
                dto
        );
    }

    /**
     * 小程序新建公司通知：用户在小程序里新建公司时，提示该线索的负责人
     *  "用户 XXX 已成功创建公司"。
     *
     * 复用 LeadNotificationDTO：companyName 信息装到 leadName 字段（前端识别 type 时单独取用）。
     * 简化版：只推文案，不带跳转按钮。
     */
    public void notifyAppletCreateCompany(Lead lead, String userIdentifier, String companyName) {
        if (lead == null || lead.getOwnerUser() == null || lead.getOwnerUser().isEmpty()) {
            return;
        }
        LeadNotificationDTO dto = new LeadNotificationDTO();
        dto.setLeadId(lead.getId());
        // leadName 字段复用承载"公司名"，前端按 notificationType 区分语义
        dto.setLeadName(companyName);
        dto.setContactName(userIdentifier != null ? userIdentifier : lead.getContactName());
        dto.setPhone(lead.getPhone());
        dto.setSource(lead.getSource());
        dto.setCreateTime(lead.getCreateTime());
        dto.setNotificationType("applet_create_company");

        log.debug("发送小程序创建公司通知给用户 [{}]: leadId={}, user={}, company={}",
                lead.getOwnerUser(), lead.getId(), userIdentifier, companyName);

        messagingTemplate.convertAndSendToUser(
                lead.getOwnerUser(),
                "/queue/lead-notification",
                dto
        );
    }
}
