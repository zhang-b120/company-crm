package com.qykj.finmaster.task.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-18
 * @Description
 */
@Data
public class GetCommonDetailBasicDataResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -5506873094034495249L;

    private Long id;

    /**
     * 执行顺序
     */
    private Integer sort;

    /**
     * name
     */
    private String processName;

    /**
     * 状态 字典COMMON_TASK_DETAIL_STATUS
     */
    private String status;

    /**
     * 状态名称 字典COMMON_TASK_DETAIL_STATUS
     */
    private String statusName;
}