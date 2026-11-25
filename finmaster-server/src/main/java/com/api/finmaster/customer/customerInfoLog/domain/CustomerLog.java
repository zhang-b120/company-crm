package com.qykj.finmaster.customer.customerInfoLog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 客户操作日志对象 customer_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_log")
public class CustomerLog extends BaseEntity {
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

    /** 日志类型（1创建 2跟进 3转化 4编辑 5转入公海 6领取 7分配 8废弃） */
    @TableField("log_type")
    private Integer logType;

    /** 日志内容 */
    @TableField("content")
    private String content;

    /** 跟进方式（当log_type=2时使用） */
    @TableField("follow_type")
    private String followType;

    /** 下次跟进时间（当log_type=2时使用） */
    @TableField("next_follow_time")
    private Date nextFollowTime;
} 