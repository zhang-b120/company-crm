package com.qykj.finmaster.order.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-29
 * @Description
 */
@Data
public class GetOrderDetailDataDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 9092018945395830774L;

    /**
     * 订单编码
     */
    private String orderNo;

    /**
     * 订单明细ID
     */
    private Long orderDetailId;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 负责人
     */
    private String ownerUser;

}