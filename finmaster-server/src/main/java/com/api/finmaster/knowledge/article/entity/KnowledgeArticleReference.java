package com.qykj.finmaster.knowledge.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("knowledge_article_reference")
public class KnowledgeArticleReference {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("article_id")
    private Long articleId;

    @TableField("ref_article_id")
    private Long refArticleId;

    @TableField("sort")
    private Integer sort;

    @TableField("create_time")
    private Date createTime;
}
