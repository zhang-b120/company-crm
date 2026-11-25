package com.qykj.finmaster.cs.schedule.domain.req;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class SaveScheduleConfigReq implements Serializable {
    private Long userId;
    private String userName;
    private String userAccount;
    private LocalDate startDate;
    private LocalDate endDate;
    /** AM / PM */
    private String shiftType;
    private String workStartTime;
    private String workEndTime;
    /** 时间槽间隔（分钟），默认30 */
    private Integer slotInterval;
    /** 每槽最大接单数，默认1 */
    private Integer maxCapacity;
}
