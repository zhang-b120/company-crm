package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 续费统计单月行：按 company_service.actual_end_date 的年月聚合。
 * month=YYYY-MM；serviceCount=当月命中服务笔数(COUNT(*)，与明细行数对齐)；contractAmount=当月命中服务对应合同价之和。
 */
@Data
public class RenewalStatRow {

    /** 月份 YYYY-MM */
    private String month;

    /** 服务数量（按服务笔数 COUNT(*)，与明细行数一致） */
    private long serviceCount;

    /** 合同金额（SUM(contract_price)，无则 0） */
    private BigDecimal contractAmount = BigDecimal.ZERO;

    public RenewalStatRow() {
    }

    public RenewalStatRow(String month, long serviceCount, BigDecimal contractAmount) {
        this.month = month;
        this.serviceCount = serviceCount;
        this.contractAmount = contractAmount == null ? BigDecimal.ZERO : contractAmount;
    }
}
