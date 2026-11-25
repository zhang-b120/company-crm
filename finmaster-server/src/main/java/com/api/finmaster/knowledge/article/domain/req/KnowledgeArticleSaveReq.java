package com.qykj.finmaster.knowledge.article.domain.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowledgeArticleSaveReq {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String contentText;
    private Long categoryId;
    private String coverImage;
    /** DRAFT 或 PUBLISHED */
    private String status;
    private List<String> tags;
    /** 作者手动指定的引用文章ID列表（顺序即展示顺序） */
    private List<Long> referenceIds;
}
