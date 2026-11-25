package com.qykj.finmaster.oa.approval.domain.req;

import lombok.Data;

/**
 * 发送审批催办请求
 */
@Data
public class SendReminderReq {

    /** 审批实例ID */
    private Long instanceId;

    /** 催办留言 */
    private String message;
}
