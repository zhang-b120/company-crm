package com.qykj.finmaster.oa.attendance.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 新增考勤记录请求
 */
@Data
public class AddAttendanceReq {

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户姓名 */
    private String realName;

    /** 考勤日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date attendanceDate;

    /** 签到时间 */
    @JsonFormat(pattern = "HH:mm:ss")
    private Date checkInTime;

    /** 签退时间 */
    @JsonFormat(pattern = "HH:mm:ss")
    private Date checkOutTime;

    /** 状态（NORMAL/LATE/EARLY/ABSENT） */
    private String status;
}
