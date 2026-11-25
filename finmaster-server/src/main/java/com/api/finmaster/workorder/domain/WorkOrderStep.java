package com.qykj.finmaster.workorder.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务单节点 work_order_step
 */
@Data
@TableName("work_order_step")
public class WorkOrderStep implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("step_code")
    private String stepCode;

    @TableField("step_name")
    private String stepName;

    private Integer sort;

    @TableField("is_current")
    private Integer isCurrent;

    @TableField("owner_user")
    private String ownerUser;

    @TableField("owner_user_name")
    private String ownerUserName;

    /** 枚举 WorkOrderStepStatus 的 name() */
    private String status;

    @TableField("form_data_json")
    private String formDataJson;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("start_time")
    private Date startTime;

    @TableField("complete_time")
    private Date completeTime;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
