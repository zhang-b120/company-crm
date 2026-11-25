package com.qykj.finmaster.system.role.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

import java.io.Serial;

@Data
@TableName("sys_role_menu")
public class RoleMenu extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 6453785213818342033L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("role_id")
    private Long roleId;

    @TableField("menu_id")
    private Long menuId;

    @TableLogic
    private Integer deleted;
}