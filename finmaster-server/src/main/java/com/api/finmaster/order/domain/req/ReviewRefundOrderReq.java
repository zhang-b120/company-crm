package com.qykj.finmaster.order.domain.req;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-11-21
 * @Description
 */
@Data
public class ReviewRefundOrderReq implements Serializable {
    private static final long serialVersionUID = -3554612094264767555L;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 审核结果 通过-TURE 驳回-FALSE
     */
    private Boolean result;

    /**
     * 备注
     */
    private String remark;
}