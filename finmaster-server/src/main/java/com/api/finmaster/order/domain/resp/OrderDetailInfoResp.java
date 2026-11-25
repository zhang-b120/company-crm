package com.qykj.finmaster.order.domain.resp;

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
public class OrderDetailInfoResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 5696857901147618568L;

    /** 明细ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 订单ID */
    private Long orderId;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
    /**更新**/
    private String updateByName;

    /** 产品ID */
    private Long productId;

    /** 产品名称 */
    private String productName;

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

    /** 产品编码 */
    private String productNumber;

    /**
     * 公司Id
     */
    private Long companyId;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 公司名称
     */
    private String companyTaxNumber;

    /**
     * 公司类型
     */
    private String companyType;

    /**
     * 订单明细类型 NEW-新增 RENEW-续费 BALANCE-补款
     */
    private String orderDetailType;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 业务名称
     */
    private String businessName;
}