package com.qykj.finmaster.system.product.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 产品对象 product
 */
@Data
@TableName("product_info")
@EqualsAndHashCode(callSuper = true)
public class Product extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 产品ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(value = "tenant_id")
    private Long tenantId;

    /**
     * 产品名称
     */
    @TableField(value = "product_name")
    private String productName;

    /**
     * 产品编号
     */
    @TableField(value = "product_number")
    private String productNumber;

    /**
     * 产品类型（SOFTWARE-软件产品，HARDWARE-硬件产品，SERVICE-服务）
     */
    @TableField(value = "product_type")
    private String productType;

    /**
     * 单位
     */
    @TableField(value = "unit")
    private String unit;

    /**
     * 规格
     */
    @TableField(value = "specification")
    private String specification;

    /**
     * 单价
     */
    @TableField(value = "unit_price")
    private BigDecimal unitPrice;

    /**
     * 最大折扣比例
     */
    @TableField(value = "max_discount")
    private Integer maxDiscount;

    /**
     * 产品描述
     */
    @TableField(value = "description")
    private String description;

}
