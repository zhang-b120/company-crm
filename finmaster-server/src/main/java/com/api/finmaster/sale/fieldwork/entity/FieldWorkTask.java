package com.qykj.finmaster.sale.fieldwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 外勤任务实体，映射 field_work_task 表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("field_work_task")
public class FieldWorkTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号 */
    @TableField("task_no")
    private String taskNo;

    /** 关联客户ID */
    @TableField("customer_id")
    private Long customerId;

    /** 客户名称（冗余） */
    @TableField("customer_name")
    private String customerName;

    /** 关联订单ID */
    @TableField("order_id")
    private Long orderId;

    /** 订单编号（冗余） */
    @TableField("order_no")
    private String orderNo;

    /** 任务类型（字典: field_work_task_type） */
    @TableField("task_type")
    private String taskType;

    /** 外勤负责人ID */
    @TableField("assignee_id")
    private Long assigneeId;

    /** 外勤负责人名称 */
    @TableField("assignee_name")
    private String assigneeName;

    /** 下发人ID */
    @TableField("dispatcher_id")
    private Long dispatcherId;

    /** 下发人名称 */
    @TableField("dispatcher_name")
    private String dispatcherName;

    /** 计划时长（分钟） */
    @TableField("planned_duration")
    private Integer plannedDuration;

    /** 实际时长（分钟） */
    @TableField("actual_duration")
    private Integer actualDuration;

    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("planned_start_time")
    private Date plannedStartTime;

    /** 实际开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("actual_start_time")
    private Date actualStartTime;

    /** 实际结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("actual_end_time")
    private Date actualEndTime;

    /**
     * 任务状态
     * PENDING=待处理 IN_PROGRESS=进行中 COMPLETED=已完成 CANCELLED=已取消
     */
    @TableField("status")
    private String status;

    /** 任务描述 */
    @TableField("description")
    private String description;

    /** 完工情况说明 */
    @TableField("completion_notes")
    private String completionNotes;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
