package com.qykj.finmaster.task.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-19
 * @Description
 */
@Data
public class CancelTaskReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1947146499554850186L;
    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 取消原因
     */
    private String cancelReason;
}