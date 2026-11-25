package com.qykj.finmaster.order.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 退款订单信息表
 * @Author zhb
 * @Date 2025-11-20
 * @Description
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("refund_order_info")
public class RefundOrderInfo extends BaseEntity {

    private static final long serialVersionUID = 10777283990832895L;

    /**
     * 订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 银行名
     */
    @TableField("bank_name")
    private String bankName;

    /**
     * 银行账号
     */
    @TableField("bank_account")
    private String bankAccount;

    /**
     * 户名
     */
    @TableField("bank_account_name")
    private String bankAccountName;

    /**
     * 是否转账（0否 1是）
     */
    @TableField("is_transfer")
    private Integer isTransfer;

    /**
     * 转账截图
     */
    @TableField("transfer_img")
    private String transferImg;

    /**
     * 是否取消办税（0否 1是）
     */
    @TableField("is_cancel_tax")
    private Integer isCancelTax;

    /**
     * 取消办税截图
     */
    @TableField("cancel_tax_img")
    private String cancelTaxImg;

    /**
     * 是否取消任务（0否 1是）
     */
    @TableField("is_cancel_task")
    private Integer isCancelTask;

    /**
     * 是否删除公司维护（0否 1是）
     */
    @TableField("is_delete_company")
    private Integer isDeleteCompany;

    /**
     * 是否取消账套（0否 1是）
     */
    @TableField("is_cancel_account")
    private Integer isCancelAccount;

    /**
     * 状态 REJECT-驳回 PASS-通过 REVIEW-待审核
     */
    @TableField("status")
    private String status;

    /**
     * 是否删除（0否 1是）
     */
    @TableLogic
    private Integer deleted;

}