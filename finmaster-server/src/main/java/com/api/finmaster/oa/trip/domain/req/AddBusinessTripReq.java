package com.qykj.finmaster.oa.trip.domain.req;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增出差申请请求
 */
@Data
public class AddBusinessTripReq {

    /** 主键ID（编辑时传入） */
    private Long id;

    /** 目的地 */
    private String destination;

    /** 开始日期 */
    private String startDate;

    /** 结束日期 */
    private String endDate;

    /** 出差天数 */
    private Integer durationDays;

    /** 出差事由 */
    private String purpose;

    /** 交通方式 */
    private String transportation;

    /** 预估费用 */
    private BigDecimal estimatedCost;

    /** 行程段列表 */
    private List<TripLegReq> legs;
}
