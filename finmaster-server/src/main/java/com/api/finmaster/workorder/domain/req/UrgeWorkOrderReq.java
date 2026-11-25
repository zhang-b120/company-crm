package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 催办任务（任务发起人可操作；记一条 URGE 日志，后续可挂消息通知）
 */
@Data
public class UrgeWorkOrderReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务单 ID 不能为空")
    private Long workOrderId;

    @NotBlank(message = "催办内容不能为空")
    private String content;
}
