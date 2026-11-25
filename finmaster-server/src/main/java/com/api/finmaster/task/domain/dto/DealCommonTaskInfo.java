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
public class DealCommonTaskInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 638815182652162580L;

    /**
     * 服务ID
     */
    private Long serviceId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 数量
     */
    private Integer count;
}