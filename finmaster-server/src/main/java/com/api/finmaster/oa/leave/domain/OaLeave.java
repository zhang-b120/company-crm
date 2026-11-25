package com.qykj.finmaster.oa.leave.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 请假 oa_leave
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_leave")
public class OaLeave extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 申请人账号 */
    @TableField("applicant")
    private String applicant;

    /** 申请人姓名 */
    @TableField("applicant_name")
    private String applicantName;

    /** 请假类型：字典oa_leave_type */
    @TableField("leave_type")
    private String leaveType;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("start_time")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("end_time")
    private Date endTime;

    /** 请假时长（天） */
    @TableField("duration")
    private BigDecimal duration;

    /** 请假原因 */
    @TableField("reason")
    private String reason;

    /** 状态：DRAFT-草稿，PENDING-待审批，APPROVED-已通过，REJECTED-已拒绝 */
    @TableField("status")
    private String status;

    /** 审批实例ID */
    @TableField("approval_instance_id")
    private Long approvalInstanceId;

    /** 是否半天假（1=是） */
    @TableField("half_day")
    private Integer halfDay;

    /** 半天假时段：MORNING=上午 / AFTERNOON=下午 */
    @TableField("half_day_period")
    private String halfDayPeriod;

    /** 关联加班记录ID（调休类型使用） */
    @TableField("overtime_record_id")
    private Long overtimeRecordId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
