package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户公司信息对象 customer_company
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_service_renew")
public class CompanyServiceRenew extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 2322858212624167100L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公司ID
     */
    @TableField("company_service_id")
    private Long companyServiceId;

    /**
     * 续费生效日期
     */
    @TableField("renew_effective_date")
    private LocalDate renewEffectiveDate;

    /**
     * 续费截止日期
     */
    @TableField("renew_end_date")
    private LocalDate renewEndDate;

    /**
     * 续费类型 0-正常续费 1-提前续费 2-延期续费 3-未续费
     */
    @TableField("renew_type")
    private Integer renewType;

    /**
     * 续费原因
     */
    @TableField("renew_reason")
    private String renewReason;

    /**
     * 续费原因
     */
    @TableField("renew_amount")
    private BigDecimal renewAmount;

    /**
     * 是否删除（0否 1是）
     */
    @TableLogic
    private Integer deleted;
} 