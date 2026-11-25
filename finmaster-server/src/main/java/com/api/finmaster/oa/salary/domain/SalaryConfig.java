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
 * 薪资配置 oa_salary_config
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_salary_config")
public class SalaryConfig extends BaseEntity {
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

    /** 基本工资 */
    @TableField("base_salary")
    private BigDecimal baseSalary;

    /** 新签提成比例 */
    @TableField("new_rate")
    private BigDecimal newRate;

    /** 续费提成比例 */
    @TableField("renew_rate")
    private BigDecimal renewRate;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
