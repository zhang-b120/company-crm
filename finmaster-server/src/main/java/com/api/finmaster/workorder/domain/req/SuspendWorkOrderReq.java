package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 挂起任务 / 恢复任务 共用 DTO
 * action=SUSPEND 挂起，RESUME 恢复
 */
@Data
public class SuspendWorkOrderReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务单 ID 不能为空")
    private Long workOrderId;

    /** 挂起 / 恢复 备注（自由文本） */
    private String remark;

    /** 挂起原因字典值：CUSTOMER_ABNORMAL 客户异常 / NO_TAX 未开税 / NO_CONTACT 联系不上 */
    private String suspendReason;

    /** 计划处理时间（挂起必填，便于到点提醒处理人继续跟进） */
    private Date plannedResumeTime;
}
