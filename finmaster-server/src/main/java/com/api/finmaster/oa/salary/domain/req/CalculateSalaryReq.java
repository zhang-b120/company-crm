package com.qykj.finmaster.oa.salary.domain.req;

import lombok.Data;

/**
 * 计算薪资请求
 */
@Data
public class CalculateSalaryReq {

    /** 薪资月份，格式：2026-03 */
    private String salaryMonth;
}
