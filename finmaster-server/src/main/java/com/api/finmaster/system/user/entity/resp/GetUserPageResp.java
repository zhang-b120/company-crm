package com.qykj.finmaster.system.user.entity.resp;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-07-31
 * @Description
 */
@Data
public class GetUserPageResp extends PageEntity {

    /** 用户ID */
    private Long id;

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

    /**
     * 部门名称
     */
    private String deptName;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 多部门ID列表 */
    private List<Long> departmentIds;
}