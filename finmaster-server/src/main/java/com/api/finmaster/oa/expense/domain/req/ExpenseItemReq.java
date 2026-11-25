package com.qykj.finmaster.oa.expense.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 报销明细请求
 */
@Data
public class ExpenseItemReq {

    /** 费用类型 */
    private String expenseType;

    /** 金额 */
    private BigDecimal amount;

    /** 费用日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date expenseDate;

    /** 描述 */
    private String description;
}
