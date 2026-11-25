package com.qykj.finmaster.data.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-08-28
 * @Description
 */
@Data
public class GetPaymentDataResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -3213957191938552379L;

    /**
     * 支付类型
     */
    private String paymentType;

    /**
     * 支付类型名称
     */
    private String paymentTypeName;

    /**
     * 支付金额
     */
    private BigDecimal paymentAmount;
}