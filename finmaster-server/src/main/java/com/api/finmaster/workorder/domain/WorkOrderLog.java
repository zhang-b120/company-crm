package com.qykj.finmaster.workorder.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务单日志/评论/催办 一体表 work_order_log
 */
@Data
@TableName("work_order_log")
public class WorkOrderLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("step_id")
    private Long stepId;

    /** 枚举 WorkOrderAction 的 name() */
    private String action;

    @TableField("from_status")
    private String fromStatus;

    @TableField("to_status")
    private String toStatus;

    @TableField("from_owner_user")
    private String fromOwnerUser;

    @TableField("to_owner_user")
    private String toOwnerUser;

    /** 说明 / 评论正文 / 驳回原因 / 催办内容 */
    private String content;

    private String operator;

    @TableField("operator_name")
    private String operatorName;

    @TableField("create_time")
    private Date createTime;
}
