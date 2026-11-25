package com.qykj.finmaster.oa.attendance.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 查询考勤列表请求
 */
@Data
public class GetAttendanceListReq {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 用户名 */
    private String username;

    /** 考勤日期开始 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date attendanceDateStart;

    /** 考勤日期结束 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date attendanceDateEnd;

    /** 状态 */
    private String status;
}
