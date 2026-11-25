package com.qykj.finmaster.oa.leave.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 新增请假申请请求
 */
@Data
public class AddLeaveReq {

    /** 请假类型（事假/病假/年假/调休等） */
    private String leaveType;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 请假时长（天） */
    private BigDecimal duration;

    /** 请假原因 */
    private String reason;

    /** 是否半天假（1=是） */
    private Integer halfDay;

    /** 半天假时段：MORNING=上午 / AFTERNOON=下午 */
    private String halfDayPeriod;

    /** 关联加班记录ID（调休类型） */
    private Long overtimeRecordId;
}
