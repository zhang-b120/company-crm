package com.qykj.finmaster.customer.company.domain;

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
 * 公司员工名册 company_employee
 * 代账派工时销售录入 / 财务收到节点同步到财务系统做个税申报
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_employee")
public class CompanyEmployee extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("company_id")
    private Long companyId;

    private String name;

    @TableField("id_card")
    private String idCard;

    /** 证件类型 0身份证 1非身份证（非身份证免身份证二要素校验，工单95/IDV-05，工单99/IDV-09 改名） */
    @TableField("id_type")
    private Integer idType;

    private String phone;

    /** 月工资 */
    private BigDecimal salary;

    /** 出生日期（按身份证号自动解析） */
    @TableField("birth_date")
    private java.time.LocalDate birthDate;

    /** 入职日期 */
    @TableField("onboarding_date")
    private java.time.LocalDate onboardingDate;

    @TableLogic
    private Integer deleted;
}
