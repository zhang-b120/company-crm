package com.qykj.finmaster.oa.attendance.domain;

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
import java.time.LocalDate;
import java.util.Date;

/**
 * 考勤 oa_attendance
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_attendance")
public class OaAttendance extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 用户账号 */
    @TableField("username")
    private String username;

    /** 真实姓名 */
    @TableField("real_name")
    private String realName;

    /** 考勤日期 */
    @TableField("attendance_date")
    private LocalDate attendanceDate;

    /** 签到时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("check_in_time")
    private Date checkInTime;

    /** 签退时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("check_out_time")
    private Date checkOutTime;

    /** 考勤状态：NORMAL-正常，LATE-迟到，EARLY-早退，ABSENT-缺勤，LEAVE-请假 */
    @TableField("status")
    private String status;

    /** 工作时长（小时） */
    @TableField("work_hours")
    private BigDecimal workHours;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
