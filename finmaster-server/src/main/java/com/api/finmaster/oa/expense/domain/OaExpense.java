package com.qykj.finmaster.oa.expense.domain;

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
 * 报销 oa_expense
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_expense")
public class OaExpense extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 报销单号 */
    @TableField("expense_no")
    private String expenseNo;

    /** 申请人账号 */
    @TableField("applicant")
    private String applicant;

    /** 申请人姓名 */
    @TableField("applicant_name")
    private String applicantName;

    /** 报销总金额 */
    @TableField("total_amount")
    private BigDecimal totalAmount;

    /** 状态：DRAFT-草稿，PENDING-待审批，APPROVED-已通过，REJECTED-已拒绝，PAID-已打款 */
    @TableField("status")
    private String status;

    /** 审批实例ID */
    @TableField("approval_instance_id")
    private Long approvalInstanceId;

    /** 附件列表JSON */
    @TableField("attachments")
    private String attachments;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
