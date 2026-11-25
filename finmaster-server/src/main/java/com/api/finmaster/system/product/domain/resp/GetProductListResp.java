package com.qykj.finmaster.system.product.domain.resp;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-06-19
 * @Description
 */
@Data
public class GetProductListResp implements Serializable {
    private static final long serialVersionUID = -5183114323836620140L;

    /**
     * 产品ID
     */
    private Long id;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品编号
     */
    private String productNumber;

    /**
     * 产品类型
     */
    private String productType;

    /**
     * 单位
     */
    private String unit;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 产品描述
     */
    private String description;
}
