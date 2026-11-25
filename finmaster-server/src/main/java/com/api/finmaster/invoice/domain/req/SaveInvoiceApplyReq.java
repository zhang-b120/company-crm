package com.qykj.finmaster.invoice.domain.req;

import com.qykj.finmaster.invoice.domain.dto.InvoiceApplyDetailInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-07-18
 * @Description
 */
@Data
public class SaveInvoiceApplyReq implements Serializable {
    private static final long serialVersionUID = 7557815771427271500L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 客户ID
     */
    private Long customerId;

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
     * 发票类型 字典
     */
    private String invoiceType;

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
     * 销售方抬头
     */
    private Long invoiceTitleId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 发票明细信息
     */
    private List<InvoiceApplyDetailInfo> detailInfoList;
}