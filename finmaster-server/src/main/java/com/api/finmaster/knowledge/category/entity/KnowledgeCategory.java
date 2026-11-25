package com.qykj.finmaster.knowledge.category.entity;

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
@TableName("knowledge_category")
public class KnowledgeCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("parent_id")
    private Long parentId;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("icon")
    private String icon;

    @TableField("sort")
    private Integer sort;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @TableField(exist = false)
    private List<KnowledgeCategory> children;

    @TableField(exist = false)
    private Integer articleCount;
}
