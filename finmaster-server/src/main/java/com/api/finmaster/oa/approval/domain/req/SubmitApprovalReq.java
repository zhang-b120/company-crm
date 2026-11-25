package com.qykj.finmaster.oa.approval.domain.req;

import lombok.Data;

/**
 * 提交审批请求
 */
@Data
public class SubmitApprovalReq {

    /** 业务类型（LEAVE/EXPENSE/PAYMENT等） */
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 审批标题 */
    private String title;
}
