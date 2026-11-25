package com.qykj.finmaster.knowledge.article.domain.req;

import lombok.Data;

@Data
public class KnowledgeArticleQueryReq {
    private Integer pageNum = 1;
    private Integer pageSize = 10;

    private Long categoryId;
    private String keyword;
    private Long tagId;
    private String tagName;
    private Long authorId;
    /** DRAFT / PUBLISHED / ARCHIVED；后台传不传都行；前台默认 PUBLISHED */
    private String status;
    /** latest / hot / top */
    private String sortBy;
}
