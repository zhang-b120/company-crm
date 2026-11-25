package com.qykj.finmaster.system.productType.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 产品类型对象 product_type
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_type")
public class ProductType extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -2749816128883964233L;
    /**
     * 类型ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 类型编码
     */
    private String code;

    /**
     * 描述
     */
    private String description;
}
