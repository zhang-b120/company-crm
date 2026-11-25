package com.qykj.finmaster.customer.leadLog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 线索跟进日志实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_lead_log")
public class LeadLog extends BaseEntity {

    /** 日志ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 线索ID */
    @TableField("lead_id")
    private Long leadId;

    /** 日志类型（1创建线索 2跟进记录 3分配线索 4转化线索 5有效无效标记） */
    @TableField("log_type")
    private Integer logType;

    /** 日志内容 */
    @TableField("content")
    private String content;

    /** 下次跟进时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField("next_follow_time")
    private Date nextFollowTime;

    /** 跟进方式 */
    @TableField("follow_method")
    private String followMethod;

    /** 操作人ID */
    @TableField("operator_id")
    private Long operatorId;

    /** 操作人姓名 */
    @TableField("operator_name")
    private String operatorName;
}
