package com.qykj.finmaster.data.domain.resp;

import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-12-15
 * @Description
 */
@Data
public class ExportSaleOrderListResp {

    /**
     * 负责人名称
     */
    @Excel(title = "销售负责人", field = "ownerUserName")
    private String ownerUserName;

    /**
     * 当日成交
     */
    @Excel(title = "当日成交(单)", field = "todayCount")
    private Integer todayCount = 0;

    /**
     * 当日成交额
     */
    @Excel(title = "当日成交额", field = "todayPayment")
    private BigDecimal todayPayment = BigDecimal.ZERO;

    /**
     * 昨日成交
     */
    @Excel(title = "昨日成交(单)", field = "yesterdayCount")
    private Integer yesterdayCount = 0;

    /**
     * 昨日成交额
     */
    @Excel(title = "昨日成交额", field = "yesterdayPayment")
    private BigDecimal yesterdayPayment = BigDecimal.ZERO;

    /**
     * 当月成交
     */
    @Excel(title = "当月成交(单)", field = "monthCount")
    private Integer monthCount = 0;

    /**
     * 当月成交额
     */
    @Excel(title = "当月成交额", field = "monthPayment")
    private BigDecimal monthPayment = BigDecimal.ZERO;

    /**
     * 上月成交
     */
    @Excel(title = "上月成交(单)", field = "lastMonthCount")
    private Integer lastMonthCount = 0;

    /**
     * 上月成交额
     */
    @Excel(title = "上月成交额", field = "lastMonthPayment")
    private BigDecimal lastMonthPayment = BigDecimal.ZERO;

    /**
     * 当季度成交
     */
    @Excel(title = "当季度成交(单)", field = "quarterCount")
    private Integer quarterCount = 0;

    /**
     * 当季度成交额
     */
    @Excel(title = "当季度成交额", field = "quarterPayment")
    private BigDecimal quarterPayment = BigDecimal.ZERO;

    /**
     * 当年成交
     */
    @Excel(title = "当年成交(单)", field = "yearCount")
    private Integer yearCount = 0;

    /**
     * 当年成交额
     */
    @Excel(title = "当年成交额", field = "yearPayment")
    private BigDecimal yearPayment = BigDecimal.ZERO;

    /**
     * 累计成交
     */
    @Excel(title = "累计成交(单)", field = "allCount")
    private Integer allCount = 0;

    /**
     * 累计成交额
     */
    @Excel(title = "累计成交额", field = "allPayment")
    private BigDecimal allPayment = BigDecimal.ZERO;
}