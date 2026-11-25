package com.qykj.finmaster.oa.approval.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 审批实例 oa_approval_instance
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_approval_instance")
public class ApprovalInstance extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 审批定义ID */
    @TableField("definition_id")
    private Long definitionId;

    /** 业务类型 */
    @TableField("business_type")
    private String businessType;

    /** 业务ID */
    @TableField("business_id")
    private Long businessId;

    /** 审批标题 */
    @TableField("title")
    private String title;

    /** 申请人账号 */
    @TableField("applicant")
    private String applicant;

    /** 申请人姓名 */
    @TableField("applicant_name")
    private String applicantName;

    /** 审批状态：PENDING-待审批，APPROVED-已通过，REJECTED-已拒绝，CANCELLED-已取消 */
    @TableField("status")
    private String status;

    /** 当前审批节点 */
    @TableField("current_node")
    private Integer currentNode;

    /** 最近催办时间 */
    @TableField("last_reminded_time")
    private java.util.Date lastRemindedTime;

    /** 催办次数 */
    @TableField("remind_count")
    private Integer remindCount;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
