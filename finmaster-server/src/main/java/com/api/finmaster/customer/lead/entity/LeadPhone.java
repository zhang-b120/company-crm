package com.qykj.finmaster.customer.lead.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 线索手机号子表 lead_phone
 * 一条线索可绑定多个手机号 / 微信号 / openId 通道；is_primary=1 行与 sale_lead 主表对应字段保持同步。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lead_phone")
public class LeadPhone extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 6914023758340912001L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    /** 所属线索ID */
    @TableField("lead_id")
    private Long leadId;

    @TableField("phone")
    private String phone;

    @TableField("wechat")
    private String wechat;

    /** 微信 openId */
    @TableField("open_id")
    private String openId;

    /** 是否主通道 0-否 1-是；主通道值与主表 sale_lead.phone/open_id/wechat 同步 */
    @TableField("is_primary")
    private Integer isPrimary;

    /** 默认承接客户ID：用于"无客户的源线索合并入"，后续 uaAddCustomerCompany 反查时挂到该客户 */
    @TableField("default_customer_id")
    private Long defaultCustomerId;

    /** 通道来源：lead_create / merge_from_lead / mini_program / pc_manual / migration_initial */
    @TableField("source")
    private String source;

    /** 若来自合并：源 leadId（已软删） */
    @TableField("source_lead_id")
    private Long sourceLeadId;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
