package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 取消任务（仅 PENDING 状态 + 仅发起人）
 * 父任务取消会级联取消子任务
 */
@Data
public class CancelWorkOrderReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务单 ID 不能为空")
    private Long workOrderId;

    private String reason;
}
