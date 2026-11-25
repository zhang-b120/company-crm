package com.qykj.finmaster.oa.schedule.domain;

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
 * 日程 oa_schedule
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_schedule")
public class OaSchedule extends BaseEntity {
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

    /** 用户姓名 */
    @TableField("real_name")
    private String realName;

    /** 日程标题 */
    @TableField("title")
    private String title;

    /** 日程描述 */
    @TableField("description")
    private String description;

    /** 地点 */
    @TableField("location")
    private String location;

    /** 开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("start_time")
    private Date startTime;

    /** 结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("end_time")
    private Date endTime;

    /** 是否全天（0=否，1=是） */
    @TableField("all_day")
    private Integer allDay;

    /** 日程类型：PERSONAL-个人/MEETING-会议/TASK-任务 */
    @TableField("schedule_type")
    private String scheduleType;

    /** 颜色标记 */
    @TableField("color")
    private String color;

    /** 提醒时间（分钟） */
    @TableField("reminder_minutes")
    private Integer reminderMinutes;

    /** 是否已发送提醒（0=否，1=是） */
    @TableField("reminder_sent")
    private Integer reminderSent;

    /** 会议室ID */
    @TableField("meeting_room_id")
    private Long meetingRoomId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
