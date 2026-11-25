package com.qykj.finmaster.oa.approval.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 审批委托 oa_approval_delegation
 */
@Data
@TableName("oa_approval_delegation")
public class OaApprovalDelegation implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 委托人账号 */
    @TableField("delegator")
    private String delegator;

    /** 委托人姓名 */
    @TableField("delegator_name")
    private String delegatorName;

    /** 被委托人账号 */
    @TableField("delegate_to")
    private String delegateTo;

    /** 被委托人姓名 */
    @TableField("delegate_to_name")
    private String delegateToName;

    /** 委托开始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("start_date")
    private Date startDate;

    /** 委托结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("end_date")
    private Date endDate;

    /** 业务类型（多个用逗号分隔，为空表示全部） */
    @TableField("business_types")
    private String businessTypes;

    /** 是否启用（1=启用，0=禁用） */
    @TableField("enabled")
    private Integer enabled = 1;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
