package com.qykj.finmaster.workorder.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务单可选负责人白名单 work_order_role_config
 */
@Data
@TableName("work_order_role_config")
public class WorkOrderRoleConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    /** 任务类型（WorkOrderTaskType 的 name()） */
    @TableField("task_type")
    private String taskType;

    /** 节点码（WorkOrderStepCode 的 name() 或 null） */
    @TableField("step_code")
    private String stepCode;

    /** 角色 ID（先按角色圈人） */
    @TableField("role_id")
    private Long roleId;

    /** 用户 ID（角色内挑选出来的白名单成员） */
    @TableField("user_id")
    private Long userId;

    /** 0-禁用 1-启用 */
    private Integer enabled;

    @TableField("create_by")
    private String createBy;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private Date updateTime;
}
