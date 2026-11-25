package com.qykj.finmaster.order.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 服务转移工单创建请求（K=1 简化版，合同驱动）：
 * 1 工单 = 1 条转移对，不再涉及订单 / 退款单 / 续费合同。
 *
 * @Author zhb
 * @Date 2026-05-12
 */
@Data
public class AddServiceTransferOrderReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 源合同 id（入口为合同列表页，用户点击的那条合同） */
    private Long sourceContractId;

    /** 源合同明细 id（可选） */
    private Long sourceContractDetailId;

    /** 客户 ID（源合同的客户；目标公司必须同客户） */
    private Long customerId;

    /** 客户名称（冗余展示） */
    private String customerName;

    /** 转移原因 */
    private String refundReason;

    /** 备注 */
    private String remark;

    /** 审核人 username（SERVICE_TRANSFER 工单第一节点 REFUND_REVIEW 的负责人） */
    private String reviewerUser;

    /** 源公司 id（来自源合同） */
    private Long sourceCompanyId;

    /** 源业务 id（来自源合同） */
    private Long sourceBusinessId;

    /** 源 company_service id（决定剩余期来源） */
    private Long sourceCompanyServiceId;

    /** 目标公司 id（同客户名下另一家公司） */
    private Long targetCompanyId;

    /** 目标 company_service id（必须 businessId 与源相同） */
    private Long targetCompanyServiceId;

    /** 系统参考·转移月数（前端按源服务剩余期算出供用户参考） */
    private Integer transferMonths;

    /** 目标 company_service 的新 actualEndDate（用户填写，Node 3 据此覆盖目标服务的 actualEndDate） */
    private LocalDate targetActualEndDate;
}
