package com.qykj.finmaster.oa.approval.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 审批记录 oa_approval_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_approval_record")
public class ApprovalRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 审批实例ID */
    @TableField("instance_id")
    private Long instanceId;

    /** 节点序号 */
    @TableField("node_sort")
    private Integer nodeSort;

    /** 节点名称 */
    @TableField("node_name")
    private String nodeName;

    /** 审批人账号 */
    @TableField("approver")
    private String approver;

    /** 审批人姓名 */
    @TableField("approver_name")
    private String approverName;

    /** 审批动作：APPROVE-通过，REJECT-拒绝，TRANSFER-转交 */
    @TableField("action")
    private String action;

    /** 审批意见 */
    @TableField("comment")
    private String comment;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("action_time")
    private Date actionTime;

    /** 签署类型：AND=会签 / OR=或签 */
    @TableField("sign_type")
    private String signType;

    /** 是否抄送记录（1=是，0=否） */
    @TableField("is_cc")
    private Integer isCc;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
