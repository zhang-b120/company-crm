package com.qykj.finmaster.workorder.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 任务单主表 work_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
public class WorkOrder extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("task_no")
    private String taskNo;

    /** 枚举 WorkOrderTaskType 的 name() */
    @TableField("task_type")
    private String taskType;

    @TableField("sub_type")
    private String subType;

    /** 枚举 WorkOrderSourceType 的 name() */
    @TableField("source_type")
    private String sourceType;

    @TableField("source_id")
    private Long sourceId;

    @TableField("source_number")
    private String sourceNumber;

    @TableField("parent_task_id")
    private Long parentTaskId;

    @TableField("contract_id")
    private Long contractId;

    /** 关联合同号（虚拟字段：getDetail 时按 contractId 反查 Contract.contractNo 填充，DB 不存） */
    @TableField(exist = false)
    private String contractNo;

    /** 客户名称（虚拟字段：list/detail 时按 customerId 反查 CustomerInfo.customerName 填充） */
    @TableField(exist = false)
    private String customerName;

    /** 公司名称（虚拟字段：list/detail 时按 companyId 反查 CustomerCompany.companyName 填充） */
    @TableField(exist = false)
    private String companyName;

    @TableField("order_id")
    private Long orderId;

    @TableField("company_id")
    private Long companyId;

    @TableField("customer_id")
    private Long customerId;

    /** 枚举 WorkOrderStatus 的 name() */
    @TableField("status")
    private String status;

    private String title;
    private String description;

    @TableField("meta_json")
    private String metaJson;

    /** 代账任务详情页"核对无误"标记 JSON, 形如 {"company": true, "shareholders": true} */
    @TableField("verified_sections")
    private String verifiedSections;

    /** 期初数据类型 1-无期初数据 2-电子税务局同步 3-科目余额表 */
    @TableField("opening_data_type")
    private Integer openingDataType;

    /** 期初数据说明（type=1 或 2 时必填） */
    @TableField("opening_data_remark")
    private String openingDataRemark;

    /** 账套启用日期（代账任务，FINANCE_RECEIVE 节点完成时用作 createAccountSet.enableDate） */
    @TableField("account_set_enable_date")
    private java.time.LocalDate accountSetEnableDate;

    /** 账套截止日期（代账任务，默认启用日期+1年；续费时延长） */
    @TableField("account_set_expiry_date")
    private java.time.LocalDate accountSetExpiryDate;

    @TableField("owner_user")
    private String ownerUser;

    @TableField("owner_user_name")
    private String ownerUserName;

    @TableField("current_owner_user")
    private String currentOwnerUser;

    @TableField("current_owner_name")
    private String currentOwnerName;

    @TableField("sla_hours")
    private Integer slaHours;

    @TableField("due_time")
    private Date dueTime;

    @TableField("complete_time")
    private Date completeTime;

    @TableField("cancel_time")
    private Date cancelTime;

    @TableField("cancel_reason")
    private String cancelReason;

    /** 挂起原因（字典值：CUSTOMER_ABNORMAL 客户异常 / NO_TAX 未开税 / NO_CONTACT 联系不上） */
    @TableField("suspend_reason")
    private String suspendReason;

    /** 挂起备注 */
    @TableField("suspend_remark")
    private String suspendRemark;

    /** 计划处理时间（挂起时填写） */
    @TableField("planned_resume_time")
    private Date plannedResumeTime;

    /** 让步完成 0/1（5 类暂缺信息时允许节点先完成，财务后续补齐） */
    @TableField("concession_complete")
    private Integer concessionComplete;

    /** 让步项编码列表 JSON 数组：LEGAL_INFO_MISSING/EMPLOYEE_INFO_MISSING/SHAREHOLDER_MISSING/INDIVIDUAL_TAX_PASSWORD_MISSING/OPENING_DATA_MISSING */
    @TableField("concession_items")
    private String concessionItems;

    /** 让步说明（含让步项 + 自由文本，财务对接时使用） */
    @TableField("concession_reason")
    private String concessionReason;

    /** 让步完成时间 */
    @TableField("concession_time")
    private Date concessionTime;

    /** 让步项计划补齐时间（让步必填，财务到点追单） */
    @TableField("concession_planned_time")
    private Date concessionPlannedTime;

    /** 执行让步完成的处理人 username */
    @TableField("concession_user")
    private String concessionUser;

    /** 让步项是否已补齐 0/1（财务补齐 / 客户提供密码后置 1） */
    @TableField("concession_resolved")
    private Integer concessionResolved;

    @TableLogic
    private Integer deleted;
}
