package com.qykj.finmaster.system.role.entity.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-01
 * @Description
 */
@Data
public class AddRoleReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -4758224812812462744L;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 角色描述 */
    private String description;

    /** 是否启用 */
    private Boolean enabled;

    /** 工作台类型：MARKET / SALES / CS / BOSS / NONE */
    private String workbenchType;

}