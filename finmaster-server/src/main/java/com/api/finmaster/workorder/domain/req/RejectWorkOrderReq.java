package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 驳回任务
 * 多节点任务：退回上一节点
 * 单节点任务：退回发起人（主任务重置为 PENDING）
 */
@Data
public class RejectWorkOrderReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务单 ID 不能为空")
    private Long workOrderId;

    @NotBlank(message = "驳回原因不能为空")
    private String reason;
}
