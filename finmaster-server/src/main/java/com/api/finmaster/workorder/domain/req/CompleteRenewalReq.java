package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 续费任务完成：自动建续费订单 + 续费合同
 */
@Data
public class CompleteRenewalReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务单 ID 不能为空")
    private Long workOrderId;

    /** 续费合同业务类型（默认沿用原合同 businessId） */
    private Long businessId;

    /** 续费金额 */
    @NotNull(message = "续费金额不能为空")
    private BigDecimal contractPrice;

    /** 合同开始时间 */
    @NotNull(message = "合同开始时间不能为空")
    private LocalDateTime contractStartTime;

    /** 合同结束时间 */
    @NotNull(message = "合同结束时间不能为空")
    private LocalDateTime contractEndTime;

    /** 订单实付金额（默认与 contractPrice 相同） */
    private BigDecimal orderFinalAmount;

    /** 支付方式字典码（如 ALIPAY/WECHAT/BANK 等） */
    private String paymentType;

    /** 支付时间 */
    private LocalDate payTime;

    /** 服务清单 */
    private List<RenewalItem> items;

    /** 备注 */
    private String remark;

    /** 订单状态（PENDING / PAID / CANCELLED），默认 PAID */
    private String orderStatus;

    /**
     * 账套截止日期（财务系统 effectiveDate）。
     * 完成续费时由前端按"合同结束时间所在月最后一天"默认填充，可手动修改；
     * 持久化到 work_order.account_set_expiry_date，并直接同步给财务系统。
     */
    private LocalDate accountSetExpiryDate;

    @Data
    public static class RenewalItem implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long companyId;
        private Long businessId;
        private Long productId;
        private String productName;
        /** 单价（前端 unitPrice × quantity = amount） */
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private Integer quantity;
    }
}
