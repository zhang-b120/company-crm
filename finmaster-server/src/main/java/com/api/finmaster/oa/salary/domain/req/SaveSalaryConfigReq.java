package com.qykj.finmaster.oa.salary.domain.req;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 保存薪资配置请求
 */
@Data
public class SaveSalaryConfigReq {

    /** 主键ID（更新时传入） */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 用户姓名 */
    private String realName;

    /** 基本工资 */
    private BigDecimal baseSalary;

    /** 新签提成比例（百分比） */
    private BigDecimal newRate;

    /** 续费提成比例（百分比） */
    private BigDecimal renewRate;
}
