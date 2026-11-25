package com.qykj.finmaster.knowledge.request.domain.req;

import lombok.Data;

import java.util.List;

@Data
public class KnowledgeRequestSaveReq {
    private Long id;
    /** 想了解什么 / 标题 */
    private String title;
    /** 遇到的问题描述 */
    private String problemDescription;
    /** 期望分类 */
    private Long expectCategoryId;
    /** 期望标签 */
    private List<String> expectTags;
}
