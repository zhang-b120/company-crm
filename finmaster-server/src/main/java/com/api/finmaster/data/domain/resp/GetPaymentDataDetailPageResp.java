package com.qykj.finmaster.data.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-28
 * @Description
 */
@Data
public class GetPaymentDataDetailPageResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -5368238927370740056L;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品名称
     */
    private String productName;


    /**
     * 订单明细类型 字典
     */
    private String orderDetailType;

    /**
     * 订单明细类型名称
     */
    private String orderDetailTypeName;

    /**
     * 支付时间
     */
    private LocalDate payTime;

    /**
     * 支付类型
     */
    private String paymentType;

    /**
     * 支付类型名称
     */
    private String paymentTypeName;

    /**
     * 销售负责人
     */
    private String salesPerson;

    /**
     * 线索月份
     */
    private String leadMonth;
}