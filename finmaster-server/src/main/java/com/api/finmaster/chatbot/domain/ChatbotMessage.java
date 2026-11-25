package com.qykj.finmaster.chatbot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("chatbot_message")
public class ChatbotMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long conversationId;

    private Long tenantId;

    /** user / assistant / tool */
    private String role;

    private String content;

    /** tool 消息使用 */
    private String toolCallId;

    /** tool 消息使用 */
    private String toolName;

    /** assistant 消息中包含 tool_calls 时的原始 JSON */
    private String toolCallsJson;

    private Date createTime;

    private Integer deleted;
}
