package com.qykj.finmaster.system.tenant.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 租户实体类
 * 对应表：sys_tenant
 */
@Data
@TableName("sys_tenant")
public class Tenant extends BaseEntity {
    @Serial
    private static final long serialVersionUID = -7427461128655058549L;

    /** 租户ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /** 租户编码 */
    @TableField("tenant_code")
    private String tenantCode;
    
    /** 租户名称 */
    @TableField("tenant_name")
    private String tenantName;
    
    /** 联系人 */
    @TableField("contact_person")
    private String contactPerson;
    
    /** 联系电话 */
    @TableField("contact_phone")
    private String contactPhone;
    
    /** 联系邮箱 */
    @TableField("contact_email")
    private String contactEmail;
    
    /** 地址 */
    @TableField("address")
    private String address;
    
    /** 营业执照号 */
    @TableField("business_license")
    private String businessLicense;
    
    /** 状态（0：禁用 1：启用） */
    @TableField("status")
    private Integer status;
    
    /** 过期时间 */
    @TableField("expire_time")
    private LocalDateTime expireTime;
    
    /** 最大用户数 */
    @TableField("max_user_count")
    private Integer maxUserCount;
    
    /** 最大存储空间（字节） */
    @TableField("max_storage_size")
    private Long maxStorageSize;
    
    /** 租户Logo */
    @TableField("logo_url")
    private String logoUrl;
    
    /** 专属域名 */
    @TableField("domain")
    private String domain;
    
    /** 是否删除 */
    @TableLogic
    private Integer deleted;

} 