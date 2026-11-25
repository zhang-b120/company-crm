package com.qykj.finmaster.system.user.entity.req;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.qykj.finmaster.system.role.entity.Role;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-07-31
 * @Description
 */
@Data
public class AddUserReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -6998164634603443596L;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

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