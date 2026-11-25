package com.qykj.finmaster.system.tenant.entity.resp;

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
public class GetTenantPageResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -3674594909687271616L;

    /** 租户ID */
    private Long id;

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

    /** 最大用户数 */
    private Integer maxUserCount;

    /** 专属域名 */
    private String domain;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 状态（0：禁用 1：启用） */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}