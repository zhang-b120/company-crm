package com.qykj.finmaster.oa.schedule.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 日程参与人 oa_schedule_participant
 */
@Data
@TableName("oa_schedule_participant")
public class OaScheduleParticipant implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 日程ID */
    @TableField("schedule_id")
    private Long scheduleId;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 用户账号 */
    @TableField("username")
    private String username;

    /** 用户姓名 */
    @TableField("real_name")
    private String realName;

    /** 状态：PENDING-待确认/ACCEPTED-已接受/DECLINED-已拒绝 */
    @TableField("status")
    private String status;
}
