package com.qykj.finmaster.task.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("common_task_detail")
@EqualsAndHashCode(callSuper=false)
public class CommonTaskDetail extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -6865692150383922673L;
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 任务Id
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 执行顺序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 是否当前流程（0否 1是）
     */
    @TableField("is_current")
    private Integer isCurrent;

    /**
     * 编码
     */
    @TableField("process_code")
    private String processCode;

    /**
     * name
     */
    @TableField("process_name")
    private String processName;

    /**
     * 处理方式 0-人工, 1-自动
     */
    @TableField("hand_method")
    private Integer handMethod;

    /**
     * 任务数据
     */
    @TableField("task_data")
    private String taskData;

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
     * 实际完成时间
     */
    @TableField("real_finish_time")
    private LocalDate realFinishTime;

    /**
     * 状态 字典COMMON_TASK_DETAIL_STATUS
     */
    @TableField("status")
    private String status;

    /**
     * 是否删除（0否 1是）
     */
    @TableField("deleted")
    private Integer deleted;

    /**
     * 任务流程完成时间
     */
    @TableField("finish_time")
    private LocalDateTime finishTime;

    /**
     * 任务流程开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 挂起原因
     */
    @TableField("putup_reason")
    private String putUpReason;
}