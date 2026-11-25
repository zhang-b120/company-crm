package com.qykj.finmaster.knowledge.comment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_comment")
public class KnowledgeComment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("article_id")
    private Long articleId;

    @TableField("parent_id")
    private Long parentId;

    @TableField("reply_to_user_id")
    private Long replyToUserId;

    @TableField("reply_to_user_name")
    private String replyToUserName;

    @TableField("content")
    private String content;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("user_avatar")
    private String userAvatar;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("dislike_count")
    private Integer dislikeCount;

    /** NORMAL / HIDDEN */
    @TableField("status")
    private String status;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField(exist = false)
    private List<KnowledgeComment> children;

    @TableField(exist = false)
    private Boolean liked;

    @TableField(exist = false)
    private Boolean disliked;
}
