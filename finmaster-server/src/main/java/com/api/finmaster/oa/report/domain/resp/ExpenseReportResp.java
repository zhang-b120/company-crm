package com.qykj.finmaster.oa.report.domain.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 费用分析报表响应
 */
@Data
public class ExpenseReportResp {

    /** 按类型分组的费用明细 [{type: "交通费", amount: 12345.00}] */
    private List<Map<String, Object>> byType;

    /** 月度趋势 [{month: "2026-01", amount: 50000.00}] */
    private List<Map<String, Object>> monthlyTrend;

    /** 报销金额Top N [{applicant: "张三", applicantName: "张三", amount: 20000.00}] */
    private List<Map<String, Object>> topApplicants;

    /** 按付款分类 [{category: "SUPPLIER", amount: 30000.00}] */
    private List<Map<String, Object>> byCategory;

    /** 汇总统计 */
    private BigDecimal totalAmount;
    private Long totalCount;
    private BigDecimal avgAmount;
}
