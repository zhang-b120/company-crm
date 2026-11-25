package com.qykj.finmaster.system.bussiness.domain;

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
 * 业务产品关联对象 sys_business_product
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_business_product")
public class SysBusinessProduct extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -5619342082734987213L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 业务ID
     */
    @TableField("business_id")
    private Long businessId;

    /**
     * 产品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 是否删除（0-否 1-是）
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}

