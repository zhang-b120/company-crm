package com.qykj.finmaster.system.menu.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

import java.io.Serial;

/**
 * 菜单实体类
 * 对应数据库表 sys_menu
 */
@Data
@TableName("sys_menu")
public class Menu extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 6188545494914759641L;
    /** 菜单ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;
    
    /** 父菜单ID */
    @TableField("parent_id")
    private Long parentId;
    
    /** 菜单名称 */
    @TableField("name")
    private String name;
    
    /** 路由路径 */
    @TableField("path")
    private String path;
    
    /** 组件路径 */
    @TableField("component")
    private String component;
    
    /** 权限标识 */
    @TableField("permission")
    private String permission;
    
    /** 菜单图标 */
    @TableField("icon")
    private String icon;
    
    /** 菜单类型（1:目录 2:菜单 3:按钮） */
    @TableField("type")
    private Integer type;
    
    /** 显示顺序 */
    @TableField("sort")
    private Integer sort;
    
    /** 是否启用 */
    @TableField("enabled")
    private Boolean enabled;
    
    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}