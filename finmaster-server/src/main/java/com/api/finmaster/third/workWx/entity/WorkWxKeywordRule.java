package com.qykj.finmaster.third.workWx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * 企业微信关键词规则（工单 77 / WXKW-01）。
 */
@Data
@TableName("work_wx_keyword_rule")
public class WorkWxKeywordRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** RENEWAL/BUSINESS/TAX_RISK/COMPLAINT/PRIVATE_DEAL */
    @TableField("rule_type")
    private String ruleType;

    @TableField("rule_name")
    private String ruleName;

    /** 关键词（逗号分隔） */
    @TableField("keywords")
    private String keywords;

    /** CONTAINS/REGEX */
    @TableField("match_mode")
    private String matchMode;

    @TableField("action_hint")
    private String actionHint;

    /** 敏感 0-否 1-是（命中仅管理员/主管可见）。列名 is_sensitive 避开 MySQL 保留字；
     *  Java 属性 sensitiveFlag 避开 MP 默认投影别名 "is_sensitive AS sensitive" 的裸保留字 1064；
     *  @JsonProperty("sensitive") 保证前端 JSON 键仍为 sensitive（零改动）。 */
    @TableField("is_sensitive")
    @JsonProperty("sensitive")
    private Integer sensitiveFlag;

    @TableField("enabled")
    private Integer enabled;

    @TableField("sort")
    private Integer sort;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
