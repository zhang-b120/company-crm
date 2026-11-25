package com.qykj.finmaster.oa.approval.domain.req;

import lombok.Data;

/**
 * 审批操作请求
 */
@Data
public class ApproveReq {

    /** 审批实例ID */
    private Long instanceId;

    /** 审批动作：APPROVE/REJECT */
    private String action;

    /** 审批意见 */
    private String comment;
}
