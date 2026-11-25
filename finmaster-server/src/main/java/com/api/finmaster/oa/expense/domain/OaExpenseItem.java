package com.qykj.finmaster.oa.expense.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报销明细 oa_expense_item
 */
@Data
@TableName("oa_expense_item")
public class OaExpenseItem {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报销单ID */
    @TableField("expense_id")
    private Long expenseId;

    /** 费用类型：字典oa_expense_type */
    @TableField("expense_type")
    private String expenseType;

    /** 金额 */
    @TableField("amount")
    private BigDecimal amount;

    /** 费用发生日期 */
    @TableField("expense_date")
    private LocalDate expenseDate;

    /** 费用说明 */
    @TableField("description")
    private String description;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
