package com.qykj.finmaster.invoice.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-07-18
 * @Description
 */
@Data
public class GetInvoiceApplyPageListReq extends PageEntity {

    private static final long serialVersionUID = -4320752970749031951L;
    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 购买方名称
     */
    private String buyerCompanyName;

    /**
     * 发票状态
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDate startDate;

    /**
     * 结束时间
     */
    private LocalDate endDate;
}