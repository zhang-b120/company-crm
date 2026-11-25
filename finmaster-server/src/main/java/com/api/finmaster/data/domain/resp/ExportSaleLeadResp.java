package com.qykj.finmaster.data.domain.resp;

import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

/**
 * @Author zhb
 * @Date 2025-12-15
 * @Description
 */
@Data
public class ExportSaleLeadResp {

    /**
     * 负责人
     */
    @Excel(title = "销售负责人", field = "ownerUserName")
    private String ownerUserName;

    /**
     * 所有线索数量
     */
    @Excel(title = "所有线索数量", field = "allLeadCount")
    private Integer allLeadCount = 0;

    /**
     * 成交线索数量
     */
    @Excel(title = "成交线索数量", field = "transactionLeadCount")
    private Integer transactionLeadCount = 0;

    /**
     * 所有客户数量
     */
    @Excel(title = "所有客户数量", field = "allCustomerCount")
    private Integer allCustomerCount = 0;

    /**
     * 成交客户数量
     */
    @Excel(title = "成交客户数量", field = "transactionCustomerCount")
    private Integer transactionCustomerCount = 0;
}
