package com.qykj.finmaster.workorder.constants;

import java.util.Arrays;
import java.util.List;

/**
 * 任务单节点编码
 */
public enum WorkOrderStepCode {
    /** 代账-客服团队长审核（首节点：审核派工资料 + 选定管家） */
    CS_LEAD_REVIEW("客服团队长审核"),
    /** 代账-管家填写（旧账导入 + 电子税局密码 + 个税密码） */
    HANDYMAN_FILL("管家填写"),
    /** 代账-财务收到（后台自动建账套 / 数据迁移 触发点） */
    FINANCE_RECEIVE("财务收到"),
    /** 工商业务处理（唯一节点） */
    BUSINESS_PROCESS("工商业务处理"),
    /** 外勤执行 */
    FIELD_EXECUTE("外勤执行"),
    /** 续费执行 */
    RENEWAL_EXECUTE("续费执行"),
    /** 付款执行 */
    PAYMENT_EXECUTE("付款执行"),
    /** 退费执行（旧单节点，保留兼容存量） */
    REFUND_EXECUTE("退费执行"),
    /** 退款·审核（负责人通过 / 驳回） */
    REFUND_REVIEW("退款审核"),
    /** 退款·取消办税（4 个开关：办税/任务/公司/账套，无需截图） */
    REFUND_CANCEL_TAX("取消办税"),
    /** 退款·退款转账（出纳实际打款，需上传转账截图） */
    REFUND_TRANSFER("退款转账"),
    /** 服务转移·审核（独立 step_code，避免与退款流程混淆） */
    SERVICE_TRANSFER_REVIEW("转移审核"),
    /** 服务转移·取消办税并完成（末节点：取消源公司办税员 + 触发实际转移） */
    SERVICE_TRANSFER_CANCEL_TAX("取消办税并完成"),
    /** OTHER 子任务执行 */
    OTHER_EXECUTE("其他任务执行"),
    /** 数据收集执行（单节点：按管家下发的银行流水上传任务） */
    DATA_COLLECT_EXECUTE("数据收集执行");

    private final String label;

    WorkOrderStepCode(String label) { this.label = label; }
    public String getLabel() { return label; }

    public static WorkOrderStepCode of(String code) {
        return Arrays.stream(values())
                .filter(s -> s.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 按 task_type 返回默认节点序列
     */
    public static List<WorkOrderStepCode> defaultStepsFor(WorkOrderTaskType type) {
        switch (type) {
            case BOOKKEEPING:
                // 客服团队长审核 → 管家填写。
                // 管家完成 HANDYMAN_FILL 时由现有逻辑另起独立的「财务收到 / 财务跟进」任务给会计，所以序列里不放 FINANCE_RECEIVE。
                return Arrays.asList(CS_LEAD_REVIEW, HANDYMAN_FILL);
            case BUSINESS_REG:
                return List.of(BUSINESS_PROCESS);
            case FIELD_WORK:
                return List.of(FIELD_EXECUTE);
            case RENEWAL:
                return List.of(RENEWAL_EXECUTE);
            case PAYMENT:
                return List.of(PAYMENT_EXECUTE);
            case REFUND:
                return Arrays.asList(REFUND_REVIEW, REFUND_CANCEL_TAX, REFUND_TRANSFER);
            case SERVICE_TRANSFER:
                // 服务转移（合同驱动版）：2 节点 = 转移审核 → 取消办税并完成；
                // 末节点 SERVICE_TRANSFER_CANCEL_TAX 由 serviceTransferStepHookSync 钩子执行实际转移动作。
                return Arrays.asList(SERVICE_TRANSFER_REVIEW, SERVICE_TRANSFER_CANCEL_TAX);
            case OTHER:
                return List.of(OTHER_EXECUTE);
            case DATA_COLLECTION:
                return List.of(DATA_COLLECT_EXECUTE);
            default:
                return List.of();
        }
    }
}
