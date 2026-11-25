package com.qykj.finmaster.chatbot.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("chatbot_conversation")
public class ChatbotConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String userId;

    private String userName;

    private String title;

    private Date createTime;

    private Date updateTime;

    @TableField("deleted")
    private Integer deleted;
}
