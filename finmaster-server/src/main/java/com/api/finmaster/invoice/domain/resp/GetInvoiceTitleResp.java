package com.qykj.finmaster.invoice.domain.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-07-18
 * @Description
 */
@Data
public class GetInvoiceTitleResp implements Serializable {
    private static final long serialVersionUID = -3804342504571368054L;

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
     * 公司名称
     */
    private String companyName;

    /**
     * 统一社会信用代码
     */
    private String taxNumber;

    /**
     * 开户行
     */
    private String bankName;

    /**
     * 开户行账号
     */
    private String bankAccount;

    /**
     * 电话
     */
    private String telephone;

    /**
     * 地址
     */
    private String address;

}