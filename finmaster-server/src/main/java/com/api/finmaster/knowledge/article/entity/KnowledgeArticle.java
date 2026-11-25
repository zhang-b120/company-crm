package com.qykj.finmaster.knowledge.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_article")
public class KnowledgeArticle extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("summary")
    private String summary;

    @TableField("content")
    private String content;

    @TableField("content_text")
    private String contentText;

    @TableField("category_id")
    private Long categoryId;

    @TableField("cover_image")
    private String coverImage;

    @TableField("author_id")
    private Long authorId;

    @TableField("author_name")
    private String authorName;

    /** DRAFT / PUBLISHED / ARCHIVED */
    @TableField("status")
    private String status;

    @TableField("is_top")
    private Boolean isTop;

    @TableField("is_recommended")
    private Boolean isRecommended;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("like_count")
    private Integer likeCount;

    @TableField("dislike_count")
    private Integer dislikeCount;

    @TableField("comment_count")
    private Integer commentCount;

    @TableField("favorite_count")
    private Integer favoriteCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("publish_time")
    private Date publishTime;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==== 非数据库字段 ====
    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private List<String> tags;

    @TableField(exist = false)
    private Boolean liked;

    @TableField(exist = false)
    private Boolean disliked;

    @TableField(exist = false)
    private Boolean favorited;

    /** 作者手动指定的引用文章简要信息（仅详情接口返回） */
    @TableField(exist = false)
    private List<KnowledgeArticle> references;
}
