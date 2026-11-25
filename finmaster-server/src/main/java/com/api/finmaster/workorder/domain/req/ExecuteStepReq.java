package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 执行当前节点（节点负责人点"完成"）
 */
@Data
public class ExecuteStepReq implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotNull(message = "任务单 ID 不能为空")
    private Long workOrderId;

    /** 当前节点填写的数据 JSON */
    private String formDataJson;

    /** 备注 / 结果说明（记到 log） */
    private String remark;

    /** 完成时显式指派下一节点的负责人（可选）。代账管家完成时通常用它指派"会计/财务"。
     *  若不传，则按原逻辑：代账 FINANCE_RECEIVE 按税局省份自动分配 / 其它任务回退到发起人 */
    private String nextStepOwner;

    /** 让步完成：5 类暂缺信息时允许节点先完成，由财务后续补齐 */
    private Integer concessionComplete;

    /** 让步说明（必填当 concessionComplete=1 时），含让步项目+自由文本，便于财务后期跟进对接 */
    private String concessionReason;

    /**
     * 让步项目编码列表（与前端 CONCESSION_OPTIONS 一致）
     * LEGAL_INFO_MISSING / EMPLOYEE_INFO_MISSING / SHAREHOLDER_MISSING /
     * INDIVIDUAL_TAX_PASSWORD_MISSING / OPENING_DATA_MISSING
     * 财务系统对接时按这些编码精准识别哪些字段未填，再做差异同步
     */
    private java.util.List<String> concessionItems;

    /** 让步完成的计划补齐时间（让步必填，财务到点追单） */
    private java.util.Date concessionPlannedTime;
}
