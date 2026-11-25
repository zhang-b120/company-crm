package com.qykj.finmaster.third.workWx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * 企业微信消息关键词命中（工单 77 / WXKW-01）。
 */
@Data
@TableName("work_wx_message_keyword_hit")
public class WorkWxMessageKeywordHit {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("message_id")
    private Long messageId;

    @TableField("msg_id")
    private String msgId;

    @TableField("rule_id")
    private Long ruleId;

    @TableField("rule_type")
    private String ruleType;

    @TableField("hit_keyword")
    private String hitKeyword;

    @TableField("room_id")
    private String roomId;

    @TableField("from_type")
    private Integer fromType;

    /** 敏感（列名 is_sensitive 避开 MySQL 保留字；属性 sensitiveFlag 避开 MP 投影别名 1064；
     *  @JsonProperty("sensitive") 保证前端 JSON 键不变） */
    @TableField("is_sensitive")
    @JsonProperty("sensitive")
    private Integer sensitiveFlag;

    /** RULE/AI（二期预留） */
    @TableField("source")
    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("hit_time")
    private Date hitTime;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
}
