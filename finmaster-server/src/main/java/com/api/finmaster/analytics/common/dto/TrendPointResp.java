package com.qykj.finmaster.analytics.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 趋势图单个数据点的统一格式 {label, value, extra}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointResp {

    /** X 轴标签，如 "03-15" 或 "2026-W12" 或 "2026-03" */
    private String label;

    /** 主数值 */
    private BigDecimal value;

    /** 次数值，如数量（value 为金额时） */
    private Long count;
}
