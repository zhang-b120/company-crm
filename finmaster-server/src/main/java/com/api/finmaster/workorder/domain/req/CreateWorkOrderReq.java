package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 创建任务单（顶级任务）
 */
@Data
public class CreateWorkOrderReq implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 任务类型（WorkOrderTaskType name） */
    @NotBlank(message = "任务类型不能为空")
    private String taskType;

    /** 子类型（如工商任务区分 THIRD/SELF；其它任务也可附加） */
    private String subType;

    /** 来源类型（WorkOrderSourceType） */
    private String sourceType;

    /** 来源单据 ID（合同/订单/父任务） */
    private Long sourceId;
    private String sourceNumber;

    private Long contractId;
    private Long orderId;
    private Long companyId;
    private Long customerId;

    private String title;
    private String description;
    /** 备注（可由前端自动整理父任务/相关合同摘要写入） */
    private String remark;

    /** 任务级表单字段（按类型不同）JSON 字符串 */
    private String metaJson;

    /**
     * 单节点任务时指派的处理人 username
     * 多节点任务（代账）推荐用下面的 stepOwners 把每个节点都指派好；
     * 若 stepOwners 为空且这里只传了 assignedOwnerUser，则只填第一节点
     */
    private String assignedOwnerUser;

    /**
     * 各节点负责人指派（多节点任务用，如 BOOKKEEPING 两节点都在这里指派）
     * 示例：[{stepCode:"HANDYMAN_FILL", ownerUser:"userA"}, {stepCode:"FINANCE_RECEIVE", ownerUser:"userB"}]
     */
    private List<StepOwnerAssignment> stepOwners;

    /** SLA 时长（小时，可选） */
    private Integer slaHours;

    /**
     * 代账（BOOKKEEPING）派工专用负载：含公司/法人、股东、员工、税务凭证
     * 提交时后端事务里先 upsert 这 4 块，再创建 work_order
     * 其它任务类型忽略此字段
     */
    private BookkeepingDispatchPayload bookkeepingPayload;
}
