package com.qykj.finmaster.data.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 数据总览-日/月维度数据项
 */
@Data
public class OrderOverviewItemResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 日期标签（日: yyyy-MM-dd, 月: yyyy-MM） */
    private String label;

    /** 成交订单数 */
    private Integer orderCount = 0;

    /** 成交业绩金额 */
    private BigDecimal orderAmount = BigDecimal.ZERO;

    /** 退款单数 */
    private Integer refundCount = 0;

    /** 退款金额 */
    private BigDecimal refundAmount = BigDecimal.ZERO;
}
