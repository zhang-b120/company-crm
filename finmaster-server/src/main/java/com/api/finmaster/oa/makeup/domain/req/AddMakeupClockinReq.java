package com.qykj.finmaster.oa.makeup.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

/**
 * 新增补卡申请请求
 */
@Data
public class AddMakeupClockinReq {

    /** 考勤日期 */
    private String attendanceDate;

    /** 打卡类型：CHECK_IN-签到，CHECK_OUT-签退 */
    private String clockinType;

    /** 打卡时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String clockinTime;

    /** 补卡原因 */
    private String reason;
}
