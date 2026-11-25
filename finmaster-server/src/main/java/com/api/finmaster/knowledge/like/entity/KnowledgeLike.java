package com.qykj.finmaster.knowledge.like.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("knowledge_like")
public class KnowledgeLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** ARTICLE / COMMENT */
    @TableField("target_type")
    private String targetType;

    @TableField("target_id")
    private Long targetId;

    /** LIKE / DISLIKE */
    @TableField("action_type")
    private String actionType;

    @TableField("user_id")
    private Long userId;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("create_time")
    private Date createTime;
}
