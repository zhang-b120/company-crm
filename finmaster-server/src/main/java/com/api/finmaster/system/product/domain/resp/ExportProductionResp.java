package com.qykj.finmaster.system.product.domain.resp;

import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ExportProductionResp implements Serializable {

    private static final long serialVersionUID = 2184316579157461775L;
    /**
     * 产品ID
     */
    @Excel(title = "产品ID", field = "id")
    private Long id;

    /**
     * 租户ID
     */
    @Excel(title = "租户ID", field = "tenantId")
    private Long tenantId;

    /**
     * 产品名称
     */
    @Excel(title = "产品名称", field = "productName")
    private String productName;

    /**
     * 产品编号
     */
    @Excel(title = "产品编号", field = "productNumber")
    private String productNumber;

    /**
     * 产品类型（SOFTWARE-软件产品，HARDWARE-硬件产品，SERVICE-服务）
     */
    @Excel(title = "产品类型", field = "productType")
    private String productType;

    /**
     * 单位
     */
    @Excel(title = "单位", field = "unit")
    private String unit;

    /**
     * 规格
     */
    @Excel(title = "规格", field = "specification")
    private String specification;

    /**
     * 单价
     */
    @Excel(title = "单价", field = "unitPrice")
    private BigDecimal unitPrice;

    /**
     * 最大折扣比例
     */
    @Excel(title = "最大折扣比例", field = "maxDiscount")
    private Integer maxDiscount;

    /**
     * 产品描述
     */
    @Excel(title = "产品描述", field = "description")
    private String description;
}
