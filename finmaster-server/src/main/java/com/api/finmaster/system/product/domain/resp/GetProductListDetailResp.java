package com.qykj.finmaster.system.product.domain.resp;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class GetProductListDetailResp implements Serializable {
    private static final long serialVersionUID = 1852984399950947807L;
    /**
     * 产品ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品编号
     */
    private String productNumber;

    /**
     * 产品类型（SOFTWARE-软件产品，HARDWARE-硬件产品，SERVICE-服务）
     */
    private String productType;

    /**
     * 单位
     */
    private String unit;

    private String unitId;

    /**
     * 规格
     */
    private String specification;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 最大折扣比例
     */
    private Integer maxDiscount;

    /**
     * 产品描述
     */
    private String description;
}
