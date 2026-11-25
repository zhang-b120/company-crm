package com.qykj.finmaster.task.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-09
 * @Description
 */
@Data
public class AddCommonTaskResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -8479632515709233779L;

    /**
     * 任务Id
     */
    private Long taskId;

    /**
     * 任务明细Id
     */
    private Long taskDetailId;
}