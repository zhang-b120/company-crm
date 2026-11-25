package com.qykj.finmaster.system.tenant.entity.resp;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
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
public class GetTenantInfoResp implements Serializable {
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

    /** 备注 */
    private String remark;

}