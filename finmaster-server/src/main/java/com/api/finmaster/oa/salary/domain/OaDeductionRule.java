package com.qykj.finmaster.oa.salary.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 薪资扣款规则（租户级） oa_deduction_rule
 */
@Data
@TableName("oa_deduction_rule")
public class OaDeductionRule implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    /** 每次迟到扣款金额 */
    @TableField("late_deduction_per_time")
    private BigDecimal lateDeductionPerTime;

    /** 每天旷工扣款金额 */
    @TableField("absent_deduction_per_day")
    private BigDecimal absentDeductionPerDay;

    @TableField("update_time")
    private Date updateTime;
}
