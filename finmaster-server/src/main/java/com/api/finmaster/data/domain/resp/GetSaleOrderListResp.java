package com.qykj.finmaster.data.domain.resp;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-12-15
 * @Description
 */
@Data
public class GetSaleOrderListResp {

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

    /**
     * 当日成交
     */
    private Integer todayCount = 0;

    /**
     * 当日成交额
     */
    private BigDecimal todayPayment = BigDecimal.ZERO;

    /**
     * 昨日成交
     */
    private Integer yesterdayCount = 0;

    /**
     * 昨日成交额
     */
    private BigDecimal yesterdayPayment = BigDecimal.ZERO;

    /**
     * 当月成交
     */
    private Integer monthCount = 0;

    /**
     * 当月成交额
     */
    private BigDecimal monthPayment = BigDecimal.ZERO;

    /**
     * 上月成交
     */
    private Integer lastMonthCount = 0;

    /**
     * 上月成交额
     */
    private BigDecimal lastMonthPayment = BigDecimal.ZERO;

    /**
     * 当季度交
     */
    private Integer quarterCount = 0;

    /**
     * 当季度交额
     */
    private BigDecimal quarterPayment = BigDecimal.ZERO;

    /**
     * 当年成交
     */
    private Integer yearCount = 0;

    /**
     * 当年成交额
     */
    private BigDecimal yearPayment = BigDecimal.ZERO;

    /**
     * 累计成交
     */
    private Integer allCount = 0;

    /**
     * 累计成交额
     */
    private BigDecimal allPayment = BigDecimal.ZERO;
}