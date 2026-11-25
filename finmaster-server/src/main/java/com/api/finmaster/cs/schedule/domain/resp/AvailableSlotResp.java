package com.qykj.finmaster.cs.schedule.domain.resp;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@Data
public class AvailableSlotResp implements Serializable {
    /** 管家用户ID */
    private Long userId;
    private String userName;
    private String userAccount;
    private LocalDate workDate;
    /** AM/PM */
    private String shiftType;
    private String shiftTypeName;
    /** 该管家该班次下的可用时间槽 */
    private List<SlotItem> slots;

    @Data
    public static class SlotItem implements Serializable {
        private Long slotId;
        private String slotTime;
        private Integer maxCapacity;
        private Integer bookedCount;
        private Integer remainCount;
        private Boolean isFull;
    }
}
