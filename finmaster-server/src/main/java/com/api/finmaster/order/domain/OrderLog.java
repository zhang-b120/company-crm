package com.qykj.finmaster.order.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 订单日志对象 order_logs
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderLog extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 客户ID */
    @TableField("customer_id")
    private Long customerId;

    /** 日志类型（1创建订单 2编辑订单 3删除订单 4状态变更） */
    @TableField("log_type")
    private String logType;

    /** 日志内容 */
    @TableField("content")
    private String content;

    /** 操作人账号 */
    @TableField("operator")
    private String operator;

    /** 操作人姓名 */
    @TableField("operator_name")
    private String operatorName;
} 