package com.qykj.finmaster.system.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

/**
 * 用户部门关联实体类
 * 对应表：sys_user_department
 */
@Data
@TableName("sys_user_department")
public class UserDepartment extends BaseEntity {
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;
    
    /** 用户ID */
    @TableField("user_id")
    private Long userId;
    
    /** 部门ID */
    @TableField("department_id")
    private Long departmentId;
    
    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
} 