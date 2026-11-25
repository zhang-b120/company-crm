package com.qykj.finmaster.knowledge.request.entity;

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
@TableName("knowledge_request")
public class KnowledgeRequest extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("title")
    private String title;

    @TableField("problem_description")
    private String problemDescription;

    @TableField("expect_category_id")
    private Long expectCategoryId;

    /** 期望标签，逗号分隔 */
    @TableField("expect_tags")
    private String expectTags;

    /** PENDING / IN_PROGRESS / DONE / REJECTED */
    @TableField("status")
    private String status;

    @TableField("request_user_id")
    private Long requestUserId;

    @TableField("request_user_name")
    private String requestUserName;

    @TableField("handler_id")
    private Long handlerId;

    @TableField("handler_name")
    private String handlerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("handle_time")
    private Date handleTime;

    @TableField("handle_remark")
    private String handleRemark;

    @TableField("linked_article_id")
    private Long linkedArticleId;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 反查：分类名 */
    @TableField(exist = false)
    private String expectCategoryName;

    /** 反查：标签数组（前端用） */
    @TableField(exist = false)
    private List<String> expectTagList;

    /** 反查：关联文章标题 */
    @TableField(exist = false)
    private String linkedArticleTitle;
}
