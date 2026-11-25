package com.qykj.finmaster.oa.expense.domain.req;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 新增报销申请请求
 */
@Data
public class AddExpenseReq {

    /** 报销总金额 */
    private BigDecimal totalAmount;

    /** 附件 */
    private String attachments;

    /** 报销明细列表 */
    private List<ExpenseItemReq> items;
}
