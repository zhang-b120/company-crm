package com.qykj.finmaster.order.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-11-21
 * @Description
 */
@Data
public class RefundTransferReq implements Serializable {
    private static final long serialVersionUID = -7121045678854082836L;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 退款金额
     */
    private BigDecimal paidAmount;

    /**
     * 退款时间
     */
    private LocalDateTime payTime;

}