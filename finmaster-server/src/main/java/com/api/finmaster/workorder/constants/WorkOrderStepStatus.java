package com.qykj.finmaster.workorder.constants;

/**
 * 任务单节点状态
 */
public enum WorkOrderStepStatus {
    PENDING,   // 未开始
    RUNNING,   // 处理中
    DONE,      // 已完成
    REJECTED,  // 被驳回
    SKIPPED    // 跳过
}
