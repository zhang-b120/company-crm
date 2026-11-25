package com.qykj.finmaster.data.domain.resp;

import com.qykj.finmaster.common.annotation.Excel;
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
public class ExportPaymentDataDetailPageResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -5368238927370740056L;

    /**
     * 客户名称
     */
    @Excel(title = "客户名称", field = "customerName")
    private String customerName;

    /**
     * 产品名称
     */
    @Excel(title = "产品名称", field = "productName")
    private String productName;

    /**
     * 订单类型名称
     */
    @Excel(title = "订单类型名称", field = "orderDetailTypeName")
    private String orderDetailTypeName;

    /**
     * 支付类型名称
     */
    @Excel(title = "支付类型名称", field = "paymentTypeName")
    private String paymentTypeName;

    /**
     * 金额
     */
    @Excel(title = "金额", field = "amount")
    private BigDecimal amount;

    /**
     * 订单时间
     */
    @Excel(title = "支付时间", field = "payTime")
    private LocalDate payTime;

    /**
     * 线索月份
     */
    @Excel(title = "线索月份", field = "leadMonth")
    private String leadMonth;

    /**
     * 销售负责人
     */
    @Excel(title = "销售负责人", field = "salesPerson")
    private String salesPerson;
}