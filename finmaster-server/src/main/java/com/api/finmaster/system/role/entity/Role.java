package com.qykj.finmaster.system.role.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

import java.io.Serial;

/**
 * 角色实体类
 * 对应表：sys_role
 */
@Data
@TableName("sys_role")
public class Role extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1157338764244428845L;
    /** 角色ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;
    
    /** 角色名称 */
    @TableField("name")
    private String name;
    
    /** 角色编码 */
    @TableField("code")
    private String code;
    
    /** 角色描述 */
    @TableField("description")
    private String description;
    
    /** 排序 */
    @TableField("sort")
    private Integer sort;
    
    /** 是否启用 */
    @TableField("enabled")
    private Boolean enabled;

    /** 工作台类型：MARKET / SALES / CS / BOSS / NONE */
    @TableField("workbench_type")
    private String workbenchType;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}