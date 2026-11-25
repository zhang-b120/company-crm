package com.qykj.finmaster.order.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * @Author zhb
 * @Date 2025-08-08
 * @Description
 */
@Data
public class OrderDetailInfoReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 9077419807219787208L;

    /** 明细ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 订单ID */
    private Long orderId;

    /** 产品ID */
    private Long productId;

    /** 产品名称 */
    private String productName;

    /**
     * 产品编码
     */
    private String productNumber;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 数量 */
    private Integer quantity;

    /** 折扣比例 */
    private Integer discount;

    /** 税率 */
    private BigDecimal taxRate;

    /** 金额 */
    private BigDecimal amount;

    /** 产品规格 */
    private String specification;

    /** 单位 */
    private String unit;

    /** 产品生效日期 */
    private LocalDate effectiveDate;

    /** 产品结束日期 */
    private LocalDate endDate;

    /**
     * 公司Id
     */
    private Long companyId;

    /**
     * 业务ID 关联 sys_business.id
     */
    private Long businessId;

    /**
     * 来源明细ID
     */
    private Long sourceDetailId;
}