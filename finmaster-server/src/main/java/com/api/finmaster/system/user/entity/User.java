package com.qykj.finmaster.system.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.util.List;

/**
 * 用户实体类
 * 对应表：sys_user
 */
@Data
@TableName("sys_user")
public class User extends BaseEntity {
    @Serial
    private static final long serialVersionUID = -9157524129648230862L;

    /** 用户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 用户名 */
    @TableField("username")
    private String username;

    /** 密码 */
    @TableField("password")
    private String password;

    /** 真实姓名 */
    @TableField("real_name")
    private String realName;

    /** 邮箱 */
    @TableField("email")
    private String email;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 头像 */
    @TableField("avatar")
    private String avatar;

    /** 是否启用 */
    @TableField("enabled")
    private Boolean enabled;

    /** 所属部门ID */
    @TableField(exist = false)
    private Long deptId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    /** 多部门ID列表（非数据库字段） */
    @TableField(exist = false)
    private List<Long> departmentIds;
}
