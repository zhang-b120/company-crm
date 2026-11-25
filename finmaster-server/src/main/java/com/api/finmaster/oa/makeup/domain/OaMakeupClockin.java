package com.qykj.finmaster.oa.makeup.domain;

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
 * 补卡申请 oa_makeup_clockin
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_makeup_clockin")
public class OaMakeupClockin extends BaseEntity {
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

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 考勤日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("attendance_date")
    private Date attendanceDate;

    /** 打卡类型：CHECK_IN-签到，CHECK_OUT-签退 */
    @TableField("clockin_type")
    private String clockinType;

    /** 打卡时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("clockin_time")
    private Date clockinTime;

    /** 补卡原因 */
    @TableField("reason")
    private String reason;

    /** 状态：DRAFT-草稿，PENDING-待审批，APPROVED-已通过，REJECTED-已拒绝，CANCELLED-已取消 */
    @TableField("status")
    private String status;

    /** 审批实例ID */
    @TableField("approval_instance_id")
    private Long approvalInstanceId;

    /** 关联考勤记录ID */
    @TableField("attendance_id")
    private Long attendanceId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
