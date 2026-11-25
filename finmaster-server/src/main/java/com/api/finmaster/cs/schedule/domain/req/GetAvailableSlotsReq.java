package com.qykj.finmaster.cs.schedule.domain.req;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;

@Data
public class GetAvailableSlotsReq implements Serializable {
    /** 查询日期 */
    private LocalDate workDate;
}
