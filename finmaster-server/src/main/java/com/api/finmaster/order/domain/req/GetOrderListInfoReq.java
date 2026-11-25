package com.qykj.finmaster.order.domain.req;


import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GetOrderListInfoReq extends PageEntity {
    private static final long serialVersionUID = -7739236810580922220L;
    /** 订单编号 */
    private String orderNo;
    /** 客户ID */
    private Long customerId;
    /** 公司ID（按订单明细中的 company_id 过滤） */
    private Long companyId;
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

    /**
     * 支付方式
     */
    private String paymentType;

    /**
     * 销售负责人
     */
    private String ownerUserName;

    /**
     * 订单明细类型（NEW-新增 RENEW-续费 BALANCE-补款），仅作查询条件
     */
    private String orderDetailType;
}
