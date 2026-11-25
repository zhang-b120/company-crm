package com.qykj.finmaster.oa.payment.domain;

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
 * 付款申请 oa_payment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_payment")
public class OaPayment extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 付款单号 */
    @TableField("payment_no")
    private String paymentNo;

    /** 申请人账号 */
    @TableField("applicant")
    private String applicant;

    /** 申请人姓名 */
    @TableField("applicant_name")
    private String applicantName;

    /** 收款方名称 */
    @TableField("payee_name")
    private String payeeName;

    /** 收款方开户行 */
    @TableField("payee_bank")
    private String payeeBank;

    /** 收款方账号 */
    @TableField("payee_account")
    private String payeeAccount;

    /** 付款金额 */
    @TableField("amount")
    private BigDecimal amount;

    /** 付款用途 */
    @TableField("purpose")
    private String purpose;

    /** 状态：DRAFT-草稿，PENDING-待审批，APPROVED-已通过，REJECTED-已拒绝，PAID-已打款 */
    @TableField("status")
    private String status;

    /** 审批实例ID */
    @TableField("approval_instance_id")
    private Long approvalInstanceId;

    /** 附件列表JSON */
    @TableField("attachments")
    private String attachments;

    /** 付款分类：SUPPLIER=供应商 / EMPLOYEE=员工 / MARKETING=市场 / OTHER=其他 */
    @TableField("payment_category")
    private String paymentCategory;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
