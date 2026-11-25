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
public class PutupTaskReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 6977170910210713772L;

    /**
     * 任务id
     */
    private Long taskId;

    /**
     * 任务流程id
     */
    private Long taskDetailId;

    /**
     * 挂起原因
     */
    private String putUpReason;

    /**
     * 是否移交下一流程 0-否 1-是
     */
    private Integer isHandOverProcess;
}