package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 客户公司信息对象 customer_company
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_service")
public class CompanyService extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 5515423125261719985L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公司ID
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 客户ID
     */
    @TableField("customer_id")
    private Long customerId;

    /**
     * 来源单据号（合同编码）
     */
    @TableField("source_number")
    private String sourceNumber;

    /**
     * 来源单据ID（合同ID）
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 业务类型ID
     */
    @TableField("business_id")
    private Long businessId;

    /**
     * 任务单ID
     */
    @TableField("common_task_id")
    private Long commonTaskId;

    /**
     * 服务ID
     */
    @TableField("service_id")
    private Long serviceId;

    /**
     * 生效日期
     */
    @TableField("effective_date")
    private LocalDate effectiveDate;

    /**
     * 截止日期
     */
    @TableField("end_date")
    private LocalDate endDate;

    /**
     * 实际截止日期
     */
    @TableField("actual_end_date")
    private LocalDate actualEndDate;

    /**
     * 首次跟进时间
     */
    @TableField("first_follow_time")
    private LocalDateTime firstFollowTime;

    /**
     * 下次跟进时间
     */
    @TableField("next_follow_time")
    private LocalDateTime nextFollowTime;

    /**
     * 最近跟进时间
     */
    @TableField("last_follow_time")
    private LocalDateTime lastFollowTime;

    /**
     * 最近跟进内容
     */
    @TableField("last_follow_content")
    private String lastFollowContent;

    /**
     * 销售负责人
     */
    @TableField("sale_owner_user")
    private String saleOwnerUser;

    /**
     * 销售负责人姓名
     */
    @TableField("sale_owner_user_name")
    private String saleOwnerUserName;

    /**
     * 管家负责人
     */
    @TableField("butler_owner_user")
    private String butlerOwnerUser;

    /**
     * 管家负责人姓名
     */
    @TableField("butler_owner_user_name")
    private String butlerOwnerUserName;

    /** 待办续费任务的责任管家（V2.0.21 新增，用户名） */
    @TableField("pending_renewal_assignee")
    private String pendingRenewalAssignee;

    /** 待办续费任务的责任管家（V2.0.21 新增，真实姓名） */
    @TableField("pending_renewal_assignee_name")
    private String pendingRenewalAssigneeName;

    /** 续费任务派发时间（V2.0.21 新增） */
    @TableField("pending_renewal_assigned_at")
    private LocalDateTime pendingRenewalAssignedAt;

    /** 续费要点（V2.0.21 新增，主管派发时填写的备注） */
    @TableField("pending_renewal_note")
    private String pendingRenewalNote;

    /** 服务状态 ACTIVE 服务中 / CANCELLED 合作取消（V2.0.70，退款工单转账完成时置位） */
    @TableField("status")
    private String status;

    /**
     * 是否删除（0否 1是）
     */
    @TableLogic
    private Integer deleted;
} 