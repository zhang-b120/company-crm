package com.qykj.finmaster.task.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-16
 * @Description
 */
@Data
public class DealCommonTaskDetailInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 638815182652162580L;

    /**
     * 流程编码
     */
    private String processCode;

    /**
     * 流程名称
     */
    private String processName;

    /**
     * 数量
     */
    private Integer count;
}