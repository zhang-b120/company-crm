package com.qykj.finmaster.data.domain.resp;

import lombok.Data;

/**
 * @Author zhb
 * @Date 2025-12-15
 * @Description
 */
@Data
public class GetSaleLeadResp {

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人
     */
    private String ownerUserName;

    /**
     * 所有线索数量
     */
    private Integer allLeadCount = 0;

    /**
     * 成交线索数量
     */
    private Integer transactionLeadCount = 0;

    /**
     * 线索成交率（成交线索数量/所有线索数量）
     */
    private String leadConversionRate = "0%";

    /**
     * 全部订单数量
     */
    private Integer allOrderByLeadCount = 0;

    /**
     * 订单成交率（全部订单数量/所有线索数量）
     */
    private String orderConversionRate = "0%";
}

