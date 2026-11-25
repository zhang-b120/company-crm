package com.qykj.finmaster.oa.salary.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 薪资记录 oa_salary_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_salary_record")
public class SalaryRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 用户账号 */
    @TableField("username")
    private String username;

    /** 真实姓名 */
    @TableField("real_name")
    private String realName;

    /** 薪资月份，格式：yyyy-MM */
    @TableField("salary_month")
    private String salaryMonth;

    /** 基本工资 */
    @TableField("base_salary")
    private BigDecimal baseSalary;

    /** 新签订单金额 */
    @TableField("new_order_amount")
    private BigDecimal newOrderAmount;

    /** 新签提成 */
    @TableField("new_commission")
    private BigDecimal newCommission;

    /** 续费订单金额 */
    @TableField("renew_order_amount")
    private BigDecimal renewOrderAmount;

    /** 续费提成 */
    @TableField("renew_commission")
    private BigDecimal renewCommission;

    /** 总工资 */
    @TableField("total_salary")
    private BigDecimal totalSalary;

    /** 迟到次数 */
    @TableField("late_count")
    private Integer lateCount;

    /** 旷工天数 */
    @TableField("absent_count")
    private Integer absentCount;

    /** 迟到扣款 */
    @TableField("late_deduction")
    private BigDecimal lateDeduction;

    /** 旷工扣款 */
    @TableField("absent_deduction")
    private BigDecimal absentDeduction;

    /** 其他扣款 */
    @TableField("other_deduction")
    private BigDecimal otherDeduction;

    /** 个人社保合计 */
    @TableField("social_insurance_personal")
    private BigDecimal socialInsurancePersonal;

    /** 个人公积金 */
    @TableField("housing_fund_personal")
    private BigDecimal housingFundPersonal;

    /** 个人所得税 */
    @TableField("tax_amount")
    private BigDecimal taxAmount;

    /** 应发工资（含提成减扣款） */
    @TableField("gross_salary")
    private BigDecimal grossSalary;

    /** 实发工资（应发-社保-公积金-个税） */
    @TableField("actual_salary")
    private BigDecimal actualSalary;

    /** 工资条是否已发送 */
    @TableField("payslip_sent")
    private Integer payslipSent;

    /** 状态：DRAFT-草稿，CONFIRMED-已确认，PAID-已发放 */
    @TableField("status")
    private String status;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
