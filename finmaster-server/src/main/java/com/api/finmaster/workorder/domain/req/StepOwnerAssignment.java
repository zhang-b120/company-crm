package com.qykj.finmaster.workorder.domain.req;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建任务时为各节点指定负责人
 */
@Data
public class StepOwnerAssignment implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 节点码 WorkOrderStepCode.name() */
    private String stepCode;

    /** 负责人 username */
    private String ownerUser;
}
