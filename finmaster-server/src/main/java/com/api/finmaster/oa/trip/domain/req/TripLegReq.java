package com.qykj.finmaster.oa.trip.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 出差行程段请求
 */
@Data
public class TripLegReq {

    /** 行程顺序 */
    private Integer legOrder;

    /** 出发地 */
    private String departure;

    /** 到达地 */
    private String arrival;

    /** 出发时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String departureTime;

    /** 到达时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private String arrivalTime;

    /** 交通方式 */
    private String transportation;

    /** 预估费用 */
    private BigDecimal estimatedCost;

    /** 备注 */
    private String remark;
}
