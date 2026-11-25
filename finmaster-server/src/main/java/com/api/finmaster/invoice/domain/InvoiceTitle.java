package com.qykj.finmaster.invoice.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * 销售方抬头信息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invoice_title")
public class InvoiceTitle extends BaseEntity {

    private static final long serialVersionUID = 5187300594254740208L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公司ID
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 客户ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 公司名称
     */
    @TableField("company_name")
    private String companyName;

    /**
     * 统一社会信用代码
     */
    @TableField("tax_number")
    private String taxNumber;

    /**
     * 开户行
     */
    @TableField("bank_name")
    private String bankName;

    /**
     * 开户行账号
     */
    @TableField("bank_account")
    private String bankAccount;

    /**
     * 电话
     */
    @TableField("telephone")
    private String telephone;

    /**
     * 地址
     */
    @TableField("address")
    private String address;

    /**
     * 是否删除（0否 1是）
     */
    @TableLogic
    private Integer deleted;
} 