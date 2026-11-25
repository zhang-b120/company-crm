package com.qykj.finmaster.knowledge.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("knowledge_view_log")
public class KnowledgeViewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("article_id")
    private Long articleId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("tenant_id")
    private Long tenantId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("view_time")
    private Date viewTime;

    @TableField(exist = false)
    private String articleTitle;
}
