package com.qykj.finmaster.system.role.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

import java.io.Serial;

@Data
@TableName("sys_role_data_scope")
public class RoleDataScope extends BaseEntity {
    @Serial
    private static final long serialVersionUID = -6781315739678438307L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 角色ID
     */
    @TableField("role_id")
    private Long roleId;
    
    /**
     * 数据权限类型：1-全部数据，2-本人数据，3-本部门数据，4-指定部门数据
     */
    @TableField("data_scope")
    private Integer dataScope;
    
    /**
     * 指定部门ID列表，多个用逗号分隔
     */
    @TableField("dept_ids")
    private String deptIds;

    @TableLogic
    private Integer deleted;
} 