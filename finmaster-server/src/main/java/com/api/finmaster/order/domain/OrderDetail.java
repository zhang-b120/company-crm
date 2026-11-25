package com.qykj.finmaster.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.annotation.Excel;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单明细对象 order_detail
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_details")
public class OrderDetail extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 明细ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 产品ID */
    @TableField("product_id")
    private Long productId;

    /** 产品名称 */
    @TableField("product_name")
    private String productName;

    /** 单价 */
    @Excel(title = "单价")
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /** 数量 */
    @TableField("quantity")
    private Integer quantity;

    /** 折扣比例 */
    @TableField("discount")
    private Integer discount;

    /** 税率 */
    @TableField("tax_rate")
    private BigDecimal taxRate;

    /** 金额 */
    @TableField("amount")
    private BigDecimal amount;

    /** 产品规格 */
    @TableField("specification")
    private String specification;

    /** 单位 */
    @TableField("unit")
    private String unit;

    /** 产品生效日期 */
    @TableField("effective_date")
    private LocalDate effectiveDate;

    /** 产品结束日期 */
    @TableField("end_date")
    private LocalDate endDate;

    /** 产品编码 */
    @TableField("product_number")
    private String productNumber;

    /** 公司ID */
    @TableField("company_id")
    private Long companyId;

    /**
     * 订单明细类型 NEW-新增 RENEW-续费 BALANCE-补款
     */
    @TableField("order_detail_type")
    private String orderDetailType;

    /**
     * 业务ID 关联 sys_business.id
     */
    @TableField("business_id")
    private Long businessId;

    /**
     * 来源明细ID
     */
    @TableField("source_detail_id")
    private Long sourceDetailId;
}
