package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-25
 * @Description
 */
@Data
public class GetCompanyServiceOrderInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = -1088368141860203774L;

    private Long id;

    /**
     * 订单编码
     */
    private String orderNo;

    /**
     * 订单金额
     */
    private BigDecimal orderAmount;

    /**
     * 订单状态（PENDING待付款、PAID已付款、CANCELLED已取消）
     */
    private String orderStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 备注
     */
    private String remark;
}