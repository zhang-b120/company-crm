package com.qykj.finmaster.workbench.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 销售提成阶梯配置 commission_tier_config
 *
 * 普通业绩档（is_business_reg=0）：业绩金额落入 [min_amount, max_amount) 区间时，
 *   整笔金额 × rate_percent% 即为业绩提成
 * 工商业务档（is_business_reg=1）：所有"工商"业务订单走单一档位，
 *   (revenue - cost) × rate_percent% 即为工商业务提成
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("commission_tier_config")
public class CommissionTierConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID，0 = 全局默认 */
    @TableField("tenant_id")
    private Long tenantId;

    /** 档位名称 */
    @TableField("tier_name")
    private String tierName;

    /** 区间下限（含） */
    @TableField("min_amount")
    private BigDecimal minAmount;

    /** 区间上限（不含），null 表示无上限 */
    @TableField("max_amount")
    private BigDecimal maxAmount;

    /** 提成百分比（如 8.00 表示 8%） */
    @TableField("rate_percent")
    private BigDecimal ratePercent;

    /** 1 = 工商业务专用档 */
    @TableField("is_business_reg")
    private Integer isBusinessReg;

    /** 排序，从小到大 */
    @TableField("sort")
    private Integer sort;

    /** 是否启用 */
    @TableField("enabled")
    private Integer enabled;

    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
