package com.qykj.finmaster.system.department.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

/**
 * 部门实体类
 * 对应表：sys_department
 */
@Data
@TableName("sys_department")
public class Department extends BaseEntity {
    /** 部门ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;
    
    /** 父部门ID */
    @TableField("parent_id")
    private Long parentId;
    
    /** 部门名称 */
    @TableField("name")
    private String name;
    
    /** 部门编码 */
    @TableField("code")
    private String code;
    
    /** 负责人用户名 */
    @TableField("leader_username")
    private String leaderUsername;
    
    /** 负责人昵称 */
    @TableField("leader_real_name")
    private String leaderRealName;
    
    /** 联系电话 */
    @TableField("phone")
    private String phone;
    
    /** 邮箱 */
    @TableField("email")
    private String email;
    
    /** 排序 */
    @TableField("sort")
    private Integer sort;
    
    /** 是否启用 */
    @TableField("enabled")
    private Boolean enabled;

    /** 工作台类型：MARKET / SALES / CS / NONE */
    @TableField("workbench_type")
    private String workbenchType;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}