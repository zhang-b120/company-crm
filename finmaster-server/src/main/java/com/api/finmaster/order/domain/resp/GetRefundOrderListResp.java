package com.qykj.finmaster.order.domain.resp;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Data
public class GetRefundOrderListResp implements Serializable {
    private static final long serialVersionUID = -3391144063899267332L;
    /** 订单ID */
    private Long id;

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

    /** 最终金额 */
    private BigDecimal finalAmount;

    /** 已付金额 */
    private BigDecimal paidAmount;

    /** 订单状态（PENDING待付款、PAID已付款、CANCELLED已取消） */
    private String status;

    /** 创建者姓名 */
    private String createByName;

    /** 更新者姓名 */
    private String updateByName;

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

    private String bankName;

    private String bankAccount;

    private String bankAccountName;

    /**
     * 是否转账（0否 1是）
     */
    private Integer isTransfer;

    /**
     * 转账截图
     */
    private String transferImg;

    /**
     * 是否取消办税（0否 1是）
     */
    private Integer isCancelTax;

    /**
     * 取消办税截图
     */
    private String cancelTaxImg;

    /**
     * 是否取消任务（0否 1是）
     */
    private Integer isCancelTask;

    /**
     * 是否删除公司维护（0否 1是）
     */
    private Integer isDeleteCompany;

    /**
     * 是否取消账套（0否 1是）
     */
    private Integer isCancelAccount;

    /**
     * 退款状态
     */
    private String refundStatus;

}
