package com.qykj.finmaster.third.workWx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 企业微信群维度回复/超时指标（工单 76 / WXRESP-01）。一个待回复回合一行。
 */
@Data
@TableName("work_wx_response_metric")
public class WorkWxResponseMetric {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("room_id")
    private String roomId;

    @TableField("chat_name")
    private String chatName;

    @TableField("first_customer_msg_id")
    private String firstCustomerMsgId;

    @TableField("first_customer_user")
    private String firstCustomerUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("first_customer_time")
    private Date firstCustomerTime;

    @TableField("first_reply_msg_id")
    private String firstReplyMsgId;

    @TableField("first_reply_user")
    private String firstReplyUser;

    @TableField("first_reply_name")
    private String firstReplyName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("first_reply_time")
    private Date firstReplyTime;

    /** 首响时长（自然秒） */
    @TableField("response_seconds")
    private Long responseSeconds;

    /** 首响时长（工作时段秒） */
    @TableField("working_seconds")
    private Long workingSeconds;

    /** excellent/normal/timeout/serious_timeout/pending/unreplied_high_risk */
    @TableField("response_status")
    private String responseStatus;

    /** 是否已回复 0-否 1-是 */
    @TableField("replied")
    private Integer replied;

    /** 超时是否已弹窗提醒 0-否 1-是（WXRESP-12 告警去重；重算 upsert 时保持 null 以免被覆盖） */
    @TableField("alerted")
    private Integer alerted;

    /** 弹窗提醒时间（WXRESP-12） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("alerted_time")
    private Date alertedTime;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
