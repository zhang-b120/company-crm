package com.qykj.finmaster.registerAddress.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 注册地址对象 register_address
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("register_address")
public class RegisterAddress extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 8273498572349857234L;

    /**
     * 地址ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 地址
     */
    @TableField("address")
    private String address;

    /**
     * 状态 0-未使用 1-使用中
     */
    @TableField("status")
    private Integer status;

    /**
     * 是否启用 0-否 1-是
     */
    @TableField("enable")
    private Integer enable;

    /**
     * 是否删除（0-否 1-是）
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}

