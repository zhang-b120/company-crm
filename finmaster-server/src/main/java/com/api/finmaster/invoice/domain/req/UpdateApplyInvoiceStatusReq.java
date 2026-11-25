package com.qykj.finmaster.invoice.domain.req;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-07-21
 * @Description
 */
@Data
public class UpdateApplyInvoiceStatusReq implements Serializable {
    private static final long serialVersionUID = 8849258790436586926L;

    private Long id;

    /**
     * 发票链接
     */
    private String invoiceUrl;
}