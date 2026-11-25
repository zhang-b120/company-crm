package com.qykj.finmaster.data.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-12-15
 * @Description
 */
@Data
public class SaleOrderDataDto {

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 当日成交
     */
    private Integer todayCount;

    /**
     * 当日成交额
     */
    private BigDecimal todayPayment;

    /**
     * 昨日成交
     */
    private Integer yesterdayCount;

    /**
     * 昨日成交额
     */
    private BigDecimal yesterdayPayment;

    /**
     * 当月成交
     */
    private Integer monthCount;

    /**
     * 当月成交额
     */
    private BigDecimal monthPayment;

    /**
     * 上月成交
     */
    private Integer lastMonthCount;

    /**
     * 上月成交额
     */
    private BigDecimal lastMonthPayment;

    /**
     * 当季度交
     */
    private Integer quarterCount;

    /**
     * 当季度交额
     */
    private BigDecimal quarterPayment;

    /**
     * 当年成交
     */
    private Integer yearCount;

    /**
     * 当年成交额
     */
    private BigDecimal yearPayment;

    /**
     * 累计成交
     */
    private Integer allCount;

    /**
     * 累计成交额
     */
    private BigDecimal allPayment;
}