package com.qykj.finmaster.payment.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 支付记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_record")
public class PaymentRecord extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 支付状态 字典PAY_STATUS
     */
    @TableField("payment_status")
    private String paymentStatus;

    /**
     * 回调状态 字典NOTIFY_STATUS
     */
    @TableField("notify_status")
    private String notifyStatus;

    /**
     * 支付调起时间
     */
    @TableField("prepay_time")
    private LocalDateTime prepayTime;

    /**
     * 实际支付时间
     */
    @TableField("pay_time")
    private LocalDateTime payTime;

    /**
     * 支付超时间隔(分钟)
     */
    @TableField("time_out_interval")
    private Long timeOutInterval;

    /**
     * 支付配置ID
     */
    @TableField("payment_config_id")
    private Long paymentConfigId;

    /**
     * 支付金额 单位：分
     */
    @TableField("payment_amount")
    private Integer paymentAmount;

    /**
     * 微信支付订单号
     */
    @TableField("transaction_id")
    private String transactionId;
}
