package com.qykj.finmaster.invoice.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 发票申请信息实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("invoice_apply")
public class InvoiceApply extends BaseEntity {

    private static final long serialVersionUID = -2475548072113955838L;
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
     * 购买方名称
     */
    @TableField("buyer_company_name")
    private String buyerCompanyName;

    /**
     * 购买方统一社会信用代码
     */
    @TableField("buyer_tax_number")
    private String buyerTaxNumber;

    /**
     * 购买方开户行
     */
    @TableField("buyer_bank_name")
    private String buyerBankName;

    /**
     * 购买方开户行账号
     */
    @TableField("buyer_bank_account")
    private String buyerBankAccount;

    /**
     * 购买方电话
     */
    @TableField("buyer_telephone")
    private String buyerTelephone;

    /**
     * 购买方地址
     */
    @TableField("buyer_address")
    private String buyerAddress;

    /**
     * 购买方类型 字典
     */
    @TableField("buyer_type")
    private String buyerType;

    /**
     * 发票类型 字典
     */
    @TableField("invoice_type")
    private String invoiceType;

    /**
     * 是否为建筑类（0否 1是）
     */
    @TableField("is_architecture")
    private Integer isArchitecture;

    /**
     * 是否跨省（0否 1是）
     */
    @TableField("is_trans_province")
    private Integer isTransProvince;

    /**
     * 项目发生地
     */
    @TableField("product_location")
    private String productLocation;

    /**
     * 销售方抬头ID
     */
    @TableField("invoice_title_id")
    private Long invoiceTitleId;

    /**
     * 发票状态 字典
     */
    @TableField("status")
    private String status;

    /**
     * 发票链接
     */
    @TableField("invoice_url")
    private String invoiceUrl;

    /**
     * 是否删除（0否 1是）
     */
    @TableLogic
    private Integer deleted;
} 