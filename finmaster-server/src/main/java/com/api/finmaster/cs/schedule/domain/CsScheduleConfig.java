package com.qykj.finmaster.cs.schedule.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

@Data
@TableName("cs_schedule_config")
@EqualsAndHashCode(callSuper = false)
public class CsScheduleConfig extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("user_account")
    private String userAccount;

    @TableField("start_date")
    private LocalDate startDate;

    @TableField("end_date")
    private LocalDate endDate;

    /** 班次类型 AM/PM */
    @TableField("shift_type")
    private String shiftType;

    @TableField("work_start_time")
    private String workStartTime;

    @TableField("work_end_time")
    private String workEndTime;

    /** 时间槽间隔（分钟） */
    @TableField("slot_interval")
    private Integer slotInterval;

    /** 每槽最大接单数 */
    @TableField("max_capacity")
    private Integer maxCapacity;

    @TableField("status")
    private String status;

    @TableLogic
    private Integer deleted;
}
