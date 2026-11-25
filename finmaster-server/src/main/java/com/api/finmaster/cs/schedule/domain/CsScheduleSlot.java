package com.qykj.finmaster.cs.schedule.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;

@Data
@TableName("cs_schedule_slot")
@EqualsAndHashCode(callSuper = false)
public class CsScheduleSlot extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("schedule_config_id")
    private Long scheduleConfigId;

    @TableField("user_id")
    private Long userId;

    @TableField("user_name")
    private String userName;

    @TableField("user_account")
    private String userAccount;

    @TableField("work_date")
    private LocalDate workDate;

    /** AM/PM */
    @TableField("shift_type")
    private String shiftType;

    /** 具体时间点 如 09:00 */
    @TableField("slot_time")
    private String slotTime;

    @TableField("max_capacity")
    private Integer maxCapacity;

    @TableField("booked_count")
    private Integer bookedCount;

    @TableLogic
    private Integer deleted;
}
