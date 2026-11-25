package com.qykj.finmaster.order.domain.resp;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
@Data
public class GetOrderListResp implements Serializable {
    private static final long serialVersionUID = 3315529493757230319L;
    /** 订单ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 附件列表 */
    private String attachments;

    /** 销售负责人 */
    private String salesPerson;

    /** 更新者 */
    private String updateBy;

    private Date updateTime;
    /** 备注 */
    private String remark;

    /** 订单编号 */
    private String orderNo;

    /** 客户ID */
    private Long customerId;

    /** 客户名称 */
    private String customerName;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 折扣比例 */
    private Integer discount;

    /** 最终金额 */
    private BigDecimal finalAmount;

    /** 税额 */
    private BigDecimal taxAmount;

    /** 税后金额 */
    private BigDecimal afterTaxAmount;

    /** 已付金额 */
    private BigDecimal paidAmount;

    /** 订单状态（PENDING待付款、PAID已付款、CANCELLED已取消） */
    private String status;

    /** 创建者姓名 */
    private String createByName;

    /** 更新者姓名 */
    private String updateByName;

    /**
     * 支付方式
     */
    private String paymentType;

    /**
     * 支付方式名称
     */
    private String paymentTypeName;

    /**
     * 支付时间
     */
    private LocalDate payTime;

    /**
     * 线索月份
     */
    private String leadMonth;

    /** 订单明细列表 */
    private List<OrderDetailInfoResp> orderDetails;

    /** 是否已建过合同（前端用于隐藏「建合同」按钮）。已建过非「作废」状态合同则为 true */
    private Boolean contractCreated;
}
