package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-08-25
 * @Description
 */
@Data
public class GetCompanyServiceInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -6671857814330181641L;

    private Long id;

    /**
     * 服务名称名称
     */
    private String serviceName;

    /**
     * 合同开始日期
     */
    private LocalDate effectiveDate;

    /**
     * 实际结束日期
     */
    private LocalDate actualEndDate;

    /**
     * 服务金额
     */
    private BigDecimal serviceAmount;
}