package com.qykj.finmaster.data.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 数据总览响应
 */
@Data
public class OrderOverviewResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 当日成交订单数 */
    private Integer todayOrderCount = 0;

    /** 当日成交业绩金额 */
    private BigDecimal todayOrderAmount = BigDecimal.ZERO;

    /** 当日退款单数 */
    private Integer todayRefundCount = 0;

    /** 当日退款金额 */
    private BigDecimal todayRefundAmount = BigDecimal.ZERO;

    /** 当月成交订单数 */
    private Integer monthOrderCount = 0;

    /** 当月成交业绩金额 */
    private BigDecimal monthOrderAmount = BigDecimal.ZERO;

    /** 当月退款单数 */
    private Integer monthRefundCount = 0;

    /** 当月退款金额 */
    private BigDecimal monthRefundAmount = BigDecimal.ZERO;

    /** 近7天每日数据 */
    private List<OrderOverviewItemResp> weeklyData;

    /** 近12个月每月数据 */
    private List<OrderOverviewItemResp> yearlyData;
}
