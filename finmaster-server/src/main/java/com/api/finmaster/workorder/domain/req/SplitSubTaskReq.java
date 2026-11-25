package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 从父任务拆出子任务（可选类型：FIELD_WORK / PAYMENT / REFUND / OTHER）
 */
@Data
public class SplitSubTaskReq implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 父任务 ID */
    @NotNull(message = "父任务 ID 不能为空")
    private Long parentTaskId;

    /** 子任务类型 */
    @NotBlank(message = "子任务类型不能为空")
    private String taskType;

    /** 子任务标题 */
    private String title;

    /** 子任务描述（父任务发起人下发时写） */
    private String description;

    /** 子任务指派的处理人 */
    @NotBlank(message = "处理人不能为空")
    private String assignedOwnerUser;

    /** 子任务级表单字段 JSON（如付款金额/用途） */
    private String metaJson;

    /** 备注（自动整理父任务摘要等） */
    private String remark;

    /** 是否直接下发：true=立即推送给处理人(PENDING)；false=保存为草稿(DRAFT)，需要手动下发 */
    private Boolean dispatch;
}
