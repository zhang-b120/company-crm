package com.qykj.finmaster.order.domain.resp;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GetOrderListExportResp implements Serializable {

    private static final long serialVersionUID = -1947022111591432834L;
    // 订单信息
    @Excel(title = "订单编号", field = "orderNo")
    private String orderNo;

    @Excel(title = "客户名称", field = "customerName")
    private String customerName;

    @Excel(title = "公司名称", field = "companyName")
    private String companyName;

    @Excel(title = "税号", field = "taxNumber")
    private String taxNumber;

    @Excel(title = "销售负责人", field = "salesPerson")
    private String salesPerson;

    @Excel(title = "企业类型", field = "companyType")
    private String companyType;

    @Excel(title = "订单总金额", field = "totalAmount")
    private BigDecimal totalAmount;

    @Excel(title = "折扣比例", field = "discount")
    private Integer discount;

    @Excel(title = "最终金额", field = "finalAmount")
    private BigDecimal finalAmount;

    @Excel(title = "税额", field = "taxAmount")
    private BigDecimal taxAmount;

    @Excel(title = "税后金额", field = "afterTaxAmount")
    private BigDecimal afterTaxAmount;

    @Excel(title = "已付金额", field = "paidAmount")
    private BigDecimal paidAmount;

    @Excel(title = "订单状态", field = "status", readConverterExp = "PENDING=待付款,PAID=已付款,CANCELLED=已取消")
    private String status;

    @Excel(title = "支付时间", field = "payTime")
    private LocalDate payTime;

    @Excel(title = "线索月份", field = "leadMonth")
    private String leadMonth;

    @Excel(title = "创建者姓名", field = "createByName")
    private String createByName;

    @Excel(title = "更新者姓名", field = "updateByName")
    private String updateByName;

    // 明细信息
    @Excel(title = "产品ID", field = "productId")
    private Long productId;

    @Excel(title = "产品名称", field = "productName")
    private String productName;

    @Excel(title = "单价", field = "unitPrice")
    private BigDecimal unitPrice;

    @Excel(title = "数量", field = "quantity")
    private Integer quantity;

    @Excel(title = "明细折扣比例", field = "detailDiscount")
    private Integer detailDiscount;

    @Excel(title = "税率", field = "taxRate")
    private BigDecimal taxRate;

    @Excel(title = "金额", field = "amount")
    private BigDecimal amount;

    @Excel(title = "产品规格", field = "specification")
    private String specification;

    @Excel(title = "单位", field = "unit")
    private String unit;

    @Excel(title = "产品生效日期", field = "effectiveDate")
    private LocalDate effectiveDate;

    @Excel(title = "产品结束日期", field = "endDate")
    private LocalDate endDate;

    @Excel(title = "产品编号", field = "productNumber")
    private String productNumber;
}
