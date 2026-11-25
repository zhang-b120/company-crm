package com.qykj.finmaster.oa.report.domain.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 人力成本报表响应
 */
@Data
public class PayrollReportResp {

    /** 月度工资总额趋势 [{month: "2026-01", totalPayroll: 500000.00, headcount: 20}] */
    private List<Map<String, Object>> payrollTrend;

    /** 提成构成分析 [{month: "2026-01", baseSalary: 300000, newCommission: 100000, renewCommission: 80000}] */
    private List<Map<String, Object>> commissionBreakdown;

    /** 扣款分析 [{type: "迟到", amount: 5000}] */
    private List<Map<String, Object>> deductionAnalysis;

    /** 社保公积金成本 [{item: "养老保险(个人)", amount: 20000}, {item: "养老保险(公司)", amount: 40000}] */
    private List<Map<String, Object>> socialInsuranceCosts;

    /** 汇总 */
    private BigDecimal totalPayroll;
    private BigDecimal avgSalary;
    private Integer headcount;
}
