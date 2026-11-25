package com.qykj.finmaster.workorder.constants;

/**
 * 任务单操作类型（审计日志用）
 */
public enum WorkOrderAction {
    CREATE,     // 创建任务
    ASSIGN,     // 指派/分派负责人
    START,      // 开始任务
    EXECUTE,    // 执行节点
    SPLIT,      // 拆分子任务
    COMPLETE,   // 完成
    REJECT,     // 驳回
    SUSPEND,    // 挂起
    RESUME,     // 恢复
    CANCEL,     // 取消
    RECALL,     // 撤回（发起人，仅 PENDING 状态可撤回）
    ACCEPT,     // 受理（当前持有人确认接收，PENDING → RUNNING）
    URGE,       // 催办
    COMMENT,    // 评论
    REOPEN,     // 重新打开（老工单从 DONE/CONCESSION_DONE 反转回 RUNNING；由会计驳回 FINANCE_FOLLOW_UP 触发）
    RESUBMIT    // 重新提交（FINANCE_FOLLOW_UP 从 WAITING_FIX 回到 PENDING；由管家修正后再次完成 HANDYMAN_FILL 触发）
}
