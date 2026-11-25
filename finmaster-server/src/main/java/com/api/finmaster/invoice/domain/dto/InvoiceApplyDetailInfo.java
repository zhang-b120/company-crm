package com.qykj.finmaster.invoice.domain.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-07-18
 * @Description
 */
@Data
public class InvoiceApplyDetailInfo implements Serializable {
    private static final long serialVersionUID = -1678682761006933090L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 发票信息ID
     */
    private Long invoiceApplyId;

    /**
     * 税收商品名称
     */
    private String taxProductName;

    /**
     * 数量
     */
    private Integer num;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 单位
     */
    private String unit;

    /**
     * 规格
     */
    private String spec;

    /**
     * 税率
     */
    private Integer taxRate;

    /**
     * 发票金额(含税)
     */
    private BigDecimal amount;
}