package com.qykj.finmaster.workorder.constants;

import java.util.Arrays;

/**
 * 任务单类型
 */
public enum WorkOrderTaskType {
    BOOKKEEPING("代理记账"),
    BUSINESS_REG("工商"),
    FIELD_WORK("外勤"),
    RENEWAL("续费"),
    PAYMENT("付款"),
    REFUND("退费"),
    /** 服务转移：与退费工单流程一致，但退款金额为 0，不实际打款；
     *  剩余服务期（按月）转移给同客户名下另一公司的同业务服务（建 0 元续费合同+续费单 + 顺延 actualEndDate）。
     *  支持源订单多条明细，每条独立指定目标公司+服务，关系存 service_transfer_item 表。 */
    SERVICE_TRANSFER("服务转移"),
    /** 数据收集（银行流水上传：按管家批量下发，每个管家一个任务，下属公司逐个上传） */
    DATA_COLLECTION("数据收集"),
    /** 自由文本型子任务：仅描述 + 结果 + 附件 */
    OTHER("其他");

    private final String label;

    WorkOrderTaskType(String label) { this.label = label; }
    public String getLabel() { return label; }

    public static WorkOrderTaskType of(String code) {
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /** 是否允许作为独立顶级任务发起；OTHER 兜底任务也可以独立发起（问题描述+需求+附件） */
    public boolean canBeTopLevel() {
        return true;
    }

    /** 是否可以作为子任务被其它任务拆出（FIELD_WORK/PAYMENT/REFUND/OTHER） */
    public boolean canBeSubTask() {
        return this == FIELD_WORK || this == PAYMENT || this == REFUND || this == OTHER;
    }
}
