package com.qykj.finmaster.system.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

import java.io.Serial;

@Data
@TableName("sys_user_role")
public class UserRole extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 3621570431092207286L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("role_id")
    private Long roleId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
} 