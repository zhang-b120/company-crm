package com.qykj.finmaster.system.department.entity.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-04
 * @Description
 */
@Data
public class AddDepartmentReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -2704942398172150610L;

    /** 父部门ID */
    private Long parentId;

    /** 部门名称 */
    private String name;

    /** 部门编码 */
    private String code;

    /** 负责人用户名 */
    private String leaderUsername;

    /** 负责人昵称 */
    private String leaderRealName;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 是否启用 */
    private Boolean enabled;

    /** 工作台类型：MARKET / SALES / CS / NONE */
    private String workbenchType;
}