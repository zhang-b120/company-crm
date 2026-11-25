package com.qykj.finmaster.invoice.domain.resp;

import com.qykj.finmaster.invoice.domain.dto.InvoiceApplyDetailInfo;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-07-18
 * @Description
 */
@Data
public class GetInvoiceApplyPageListResp implements Serializable {

    private static final long serialVersionUID = -5583987297672327536L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 销售方名称
     */
    private String saleCompanyName;

    /**
     * 销售方统一社会信用代码
     */
    private String saleTaxNumber;

    /**
     * 销售方开户行
     */
    private String saleBankName;

    /**
     * 销售方开户行账号
     */
    private String saleBankAccount;

    /**
     * 销售方电话
     */
    private String saleTelephone;

    /**
     * 销售方地址
     */
    private String saleAddress;

    /**
     * 购买方名称
     */
    private String buyerCompanyName;

    /**
     * 购买方统一社会信用代码
     */
    private String buyerTaxNumber;

    /**
     * 购买方开户行
     */
    private String buyerBankName;

    /**
     * 购买方开户行账号
     */
    private String buyerBankAccount;

    /**
     * 购买方电话
     */
    private String buyerTelephone;

    /**
     * 购买方地址
     */
    private String buyerAddress;

    /**
     * 购买方类型 字典
     */
    private String buyerType;

    /**
     * 购买方类型名称
     */
    private String buyerTypeName;

    /**
     * 发票类型 字典
     */
    private String invoiceType;

    /**
     * 发票类型名称
     */
    private String invoiceTypeName;

    /**
     * 是否为建筑类（0否 1是）
     */
    private Integer isArchitecture;

    /**
     * 是否跨省（0否 1是）
     */
    private Integer isTransProvince;

    /**
     * 项目发生地
     */
    private String productLocation;

    /**
     * 销售方抬头ID
     */
    private Long invoiceTitleId;

    /**
     * 发票状态 字典
     */
    private String status;

    /**
     * 发票状态名称
     */
    private String statusName;

    /**
     * 发票链接
     */
    private String invoiceUrl;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 发票总金额
     */
    private BigDecimal amountSum;
    /**
     * 发票明细信息
     */
    private List<InvoiceApplyDetailInfo> detailInfoList;
}