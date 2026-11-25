package com.qykj.finmaster.oa.trip.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 出差申请 oa_business_trip
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_business_trip")
public class OaBusinessTrip extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 出差单号 */
    @TableField("trip_no")
    private String tripNo;

    /** 申请人账号 */
    @TableField("applicant")
    private String applicant;

    /** 申请人姓名 */
    @TableField("applicant_name")
    private String applicantName;

    /** 目的地 */
    @TableField("destination")
    private String destination;

    /** 开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("start_date")
    private Date startDate;

    /** 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("end_date")
    private Date endDate;

    /** 出差天数 */
    @TableField("duration_days")
    private Integer durationDays;

    /** 出差事由 */
    @TableField("purpose")
    private String purpose;

    /** 交通方式：FLIGHT-飞机，TRAIN-火车，CAR-自驾，OTHER-其他 */
    @TableField("transportation")
    private String transportation;

    /** 预估费用 */
    @TableField("estimated_cost")
    private BigDecimal estimatedCost;

    /** 状态：DRAFT-草稿，PENDING-待审批，APPROVED-已通过，REJECTED-已拒绝，CANCELLED-已取消 */
    @TableField("status")
    private String status;

    /** 审批实例ID */
    @TableField("approval_instance_id")
    private Long approvalInstanceId;

    /** 关联报销单ID */
    @TableField("linked_expense_id")
    private Long linkedExpenseId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
