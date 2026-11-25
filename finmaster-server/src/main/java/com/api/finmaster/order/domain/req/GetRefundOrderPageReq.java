package com.qykj.finmaster.order.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-11-20
 * @Description
 */
@Data
public class GetRefundOrderPageReq extends PageEntity {
    private static final long serialVersionUID = 6599802593953913448L;

    /** 订单编号 */
    private String orderNo;

    /** 客户名称 */
    private String customerName;

    /** 订单状态（PENDING待付款、PAID已付款、CANCELLED已取消） */
    private String status;

    /**
     * 支付时间起
     */
    private LocalDate start;

    /**
     * 支付时间止
     */
    private LocalDate end;

    /**
     * 线索月份
     */
    private String leadMonth;
}