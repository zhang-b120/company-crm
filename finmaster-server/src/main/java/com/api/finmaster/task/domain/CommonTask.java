package com.qykj.finmaster.task.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-09
 * @Description
 */
@Data
@TableName("common_task")
@EqualsAndHashCode(callSuper=false)
public class CommonTask extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 2623688466712991091L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 任务单号
     */
    @TableField("task_no")
    private String taskNo;

    /**
     * 服务ID
     */
    @TableField("service_id")
    private Long serviceId;

    /**
     * 服务名称
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 来源单据号
     */
    @TableField("source_number")
    private String sourceNumber;

    /**
     * 来源单据id
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 客户ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 公司ID
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 当前负责人
     */
    @TableField("current_owner_user")
    private String currentOwnerUser;

    /**
     * 当前负责人名称
     */
    @TableField("current_owner_user_name")
    private String currentOwnerUserName;

    /**
     * 负责人
     */
    @TableField("owner_user")
    private String ownerUser;

    /**
     * 负责人名称
     */
    @TableField("owner_user_name")
    private String ownerUserName;

    /**
     * 期望完成时间
     */
    @TableField("wish_finish_time")
    private LocalDate wishFinishTime;

    /**
     * 实际完成时间
     */
    @TableField("real_finish_time")
    private LocalDate realFinishTime;

    /**
     * 状态 字典COMMON_TASK_STATUS
     */
    @TableField("status")
    private String status;

    /**
     * 前状态 字典COMMON_TASK_STATUS
     */
    @TableField("pre_status")
    private String preStatus;

    /**
     * 是否删除（0否 1是）
     */
    @TableLogic
    private Integer deleted;

    /**
     * 任务开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 任务数据
     */
    @TableField("task_data")
    private String taskData;

    /**
     * 挂起原因
     */
    @TableField("putup_reason")
    private String putUpReason;

    /**
     * 挂起原因
     */
    @TableField("cancel_reason")
    private String cancelReason;

    /**
     * 预约的号源ID
     */
    @TableField("schedule_slot_id")
    private Long scheduleSlotId;

    /**
     * 预约处理时间
     */
    @TableField("schedule_time")
    private java.time.LocalDateTime scheduleTime;
}