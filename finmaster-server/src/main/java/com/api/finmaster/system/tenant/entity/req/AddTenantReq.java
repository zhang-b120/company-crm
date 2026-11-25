package com.qykj.finmaster.system.tenant.entity.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-01
 * @Description
 */
@Data
public class AddTenantReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -6092977399878099340L;

    /** 租户编码 */
    private String tenantCode;

    /** 租户名称 */
    private String tenantName;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 联系邮箱 */
    private String contactEmail;

    /** 地址 */
    private String address;

    /** 营业执照号 */
    private String businessLicense;

    /** 状态（0：禁用 1：启用） */
    private Integer status;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 最大用户数 */
    private Integer maxUserCount;

    /** 最大存储空间（字节） */
    private Long maxStorageSize;

    /** 租户Logo */
    private String logoUrl;

    /** 专属域名 */
    private String domain;

    /** 默认管理员用户名（必填） */
    private String defaultAdminUsername;

    /** 默认管理员密码（必填） */
    private String defaultAdminPassword;

    /** 默认管理员真实姓名（必填） */
    private String defaultAdminRealName;

    /** 默认管理员邮箱（必填） */
    private String defaultAdminEmail;

    /** 默认管理员手机号（必填） */
    private String defaultAdminPhone;

    /** 备注 */
    private String remark;
}