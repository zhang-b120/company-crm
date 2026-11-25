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
 * 审批流程定义 oa_approval_definition
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_approval_definition")
public class ApprovalDefinition extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 审批流程名称 */
    @TableField("name")
    private String name;

    /** 审批类型：LEAVE-请假，EXPENSE-报销，PAYMENT-付款 */
    @TableField("type")
    private String type;

    /** 审批节点配置JSON */
    @TableField("nodes_json")
    private String nodesJson;

    /** 是否启用：0-禁用，1-启用 */
    @TableField("enabled")
    private Integer enabled;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
