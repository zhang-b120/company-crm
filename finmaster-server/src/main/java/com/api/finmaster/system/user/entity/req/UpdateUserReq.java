package com.qykj.finmaster.system.user.entity.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-07-31
 * @Description
 */
@Data
public class UpdateUserReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 3059528983045259874L;

    private Long id;
    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 头像 */
    private String avatar;

    /** 是否启用 */
    private Boolean enabled;

    /** 多部门ID列表（非数据库字段） */
    private List<Long> departmentIds;
}