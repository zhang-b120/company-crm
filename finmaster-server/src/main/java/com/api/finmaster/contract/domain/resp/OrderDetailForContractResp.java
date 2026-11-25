package com.qykj.finmaster.contract.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同创建时可选的订单明细信息
 */
@Data
public class OrderDetailForContractResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单明细ID
     */
    private Long orderDetailId;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品编号
     */
    private String productNumber;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 截止日期
     */
    private LocalDate endDate;

    /**
     * 单价（用于前端计算合同价格：选中明细的 unit_price * quantity 求和）
     */
    private BigDecimal unitPrice;

    /**
     * 数量
     */
    private Integer quantity;
}

