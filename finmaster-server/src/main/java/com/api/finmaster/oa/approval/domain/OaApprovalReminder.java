package com.qykj.finmaster.oa.approval.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批催办记录 oa_approval_reminder
 */
@Data
@TableName("oa_approval_reminder")
public class OaApprovalReminder implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 审批实例ID */
    @TableField("instance_id")
    private Long instanceId;

    /** 催办人账号 */
    @TableField("sender")
    private String sender;

    /** 催办人姓名 */
    @TableField("sender_name")
    private String senderName;

    /** 目标审批人账号 */
    @TableField("target_approver")
    private String targetApprover;

    /** 催办时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("send_time")
    private Date sendTime;

    /** 催办留言 */
    @TableField("message")
    private String message;
}
