package com.qykj.finmaster.sale.fieldwork.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class AddFieldWorkTaskReq {

    /** 关联客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 关联订单ID（可选） */
    private Long orderId;

    /** 订单编号（可选） */
    private String orderNo;

    /** 任务类型（字典: field_work_task_type） */
    private String taskType;

    /** 外勤负责人ID */
    private Long assigneeId;

    /** 外勤负责人名称 */
    private String assigneeName;

    /** 计划时长（分钟） */
    private Integer plannedDuration;

    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date plannedStartTime;

    /** 任务描述 */
    private String description;
}
