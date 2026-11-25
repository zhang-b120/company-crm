package com.qykj.finmaster.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单对象 order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("orders")
public class Order extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 客户ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 客户名称
     */
    @TableField("customer_name")
    private String customerName;

    /**
     * 订单总金额
     */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /**
     * 折扣比例
     */
    @TableField("discount")
    private Integer discount;

    /**
     * 最终金额
     */
    @TableField("final_amount")
    private BigDecimal finalAmount;

    /**
     * 税额
     */
    @TableField("tax_amount")
    private BigDecimal taxAmount;

    /**
     * 税后金额
     */
    @TableField("after_tax_amount")
    private BigDecimal afterTaxAmount;

    /**
     * 已付金额
     */
    @TableField("paid_amount")
    private BigDecimal paidAmount;

    /**
     * 订单状态（PENDING待付款、PAID已付款、CANCELLED已取消，REFUNDING退款中， REFUNDED已退款）
     */
    @TableField("status")
    private String status;

    /**
     * 创建者姓名
     */
    @TableField("create_by_name")
    private String createByName;

    /**
     * 更新者姓名
     */
    @TableField("update_by_name")
    private String updateByName;

    /**
     * 附件列表
     */
    @TableField("attachments")
    private String attachments;

    /**
     * 公司id
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 付款方式-字典
     */
    @TableField("payment_type")
    private String paymentType;

    /**
     * 支付时间
     */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /**
     * 线索月份
     */
    @TableField("lead_month")
    private String leadMonth;

    /**
     * 订单类型：正常订单-NORMAL，退款订单-REFUND
     */
    @TableField("order_type")
    private String orderType;

    /**
     * 来源订单号
     */
    @TableField("source_order_no")
    private String sourceOrderNo;
}
