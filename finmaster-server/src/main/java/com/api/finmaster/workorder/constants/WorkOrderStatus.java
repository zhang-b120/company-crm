package com.qykj.finmaster.workorder.constants;

/**
 * 任务单主状态
 */
public enum WorkOrderStatus {
    /** 草稿（已创建但未下发，处理人不可见） */
    DRAFT,
    /** 已创建，未开始（已下发，等待处理人受理） */
    PENDING,
    /** 处理中 */
    RUNNING,
    /** 等待子任务全部完成 */
    WAITING_CHILD,
    /** 挂起 */
    SUSPENDED,
    /** 完成 */
    DONE,
    /** 让步完成（5 类必填信息有缺失但允许节点先完成，等客服跟进补齐后转为 DONE） */
    CONCESSION_DONE,
    /** 取消（发起人在 PENDING 阶段取消，或父任务取消级联） */
    CANCELED,
    /** 已驳回（首节点被驳回 → 任务整体作废，终态） */
    REJECTED,
    /** 等待管家修正（仅 FINANCE_FOLLOW_UP 子类型使用：会计驳回后老工单复活，财务跟进单进此状态等待管家二次提交。
     *  期间禁止任何操作，状态机只允许从此状态回到 PENDING） */
    WAITING_FIX;

    public boolean isTerminal() {
        return this == DONE || this == CANCELED || this == REJECTED;
    }
}
