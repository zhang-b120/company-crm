package com.qykj.finmaster.oa.schedule.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 新增/修改日程请求
 */
@Data
public class AddScheduleReq {

    /** 日程ID（修改时传入） */
    private Long id;

    /** 日程标题 */
    private String title;

    /** 日程描述 */
    private String description;

    /** 地点 */
    private String location;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /** 是否全天（0=否，1=是） */
    private Integer allDay;

    /** 日程类型：PERSONAL/MEETING/TASK */
    private String scheduleType;

    /** 颜色标记 */
    private String color;

    /** 提醒时间（分钟） */
    private Integer reminderMinutes;

    /** 会议室ID */
    private Long meetingRoomId;

    /** 参与人用户ID列表 */
    private List<Long> participantUserIds;
}
