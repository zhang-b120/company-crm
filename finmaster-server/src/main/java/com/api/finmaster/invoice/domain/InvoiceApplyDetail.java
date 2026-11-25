package com.qykj.finmaster.invoice.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 发票申请明细实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invoice_apply_detail")
public class InvoiceApplyDetail extends BaseEntity {

    private static final long serialVersionUID = -2766121552748816928L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公司ID
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 客户ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 发票信息ID
     */
    @TableField("invoice_apply_id")
    private Long invoiceApplyId;

    /**
     * 税收商品名称
     */
    @TableField("tax_product_name")
    private String taxProductName;

    /**
     * 数量
     */
    @TableField("num")
    private Integer num;

    /**
     * 单价
     */
    @TableField("unit_price")
    private BigDecimal unitPrice;

    /**
     * 单位
     */
    @TableField("unit")
    private String unit;

    /**
     * 规格
     */
    @TableField("spec")
    private String spec;

    /**
     * 税率
     */
    @TableField("tax_rate")
    private Integer taxRate;

    /**
     * 发票金额(含税)
     */
    @TableField("amount")
    private BigDecimal amount;

    /**
     * 是否删除（0否 1是）
     */
    @TableLogic
    private Integer deleted;
} 