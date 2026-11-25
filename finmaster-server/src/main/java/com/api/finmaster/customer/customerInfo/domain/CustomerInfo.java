package com.qykj.finmaster.customer.customerInfo.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 客户信息对象 customer_info
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_info")
public class CustomerInfo extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 客户ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 客户名称 */
    @TableField("customer_name")
    private String customerName;

    /** 客户类型（1私海 2公海） */
    @TableField("customer_type")
    private Integer customerType;

    /** 联系人 */
    @TableField("contact_name")
    private String contactName;

    /** 联系电话 */
    @TableField("phone")
    private String phone;

    /** 微信号（V2.0.20 新增，由线索转化时继承） */
    @TableField("wechat")
    private String wechat;

    /** 电子邮箱 */
    @TableField("email")
    private String email;

    /** 行业 */
    @TableField("industry")
    private String industry;

    /** 来源 */
    @TableField("source")
    private String source;

    /** 线索ID */
    @TableField("lead_id")
    private Long leadId;

    /** 线索名称 */
    @TableField("lead_name")
    private String leadName;

    /** 状态（1未跟进 2跟进中 3已转化 4已失效） */
    @TableField("status")
    private Integer status;

    /** 下次跟进时间 */
    @TableField("next_follow_time")
    private Date nextFollowTime;

    /** 最近跟进内容 */
    @TableField("last_follow_content")
    private String lastFollowContent;

    /** 最近跟进时间 */
    @TableField("last_follow_time")
    private Date lastFollowTime;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** open_id */
    @TableField("open_id")
    private String openId;

    @TableField("owner_user")
    private String ownerUser;

    @TableField("owner_user_name")
    private String ownerUserName;
}
