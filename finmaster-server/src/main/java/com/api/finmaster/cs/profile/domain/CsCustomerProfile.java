package com.qykj.finmaster.cs.profile.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@TableName("cs_customer_profile")
@EqualsAndHashCode(callSuper = false)
public class CsCustomerProfile extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("company_id")
    private Long companyId;

    @TableField("bookkeeping_status")
    private String bookkeepingStatus;

    @TableField("filing_status")
    private String filingStatus;

    @TableField("star_level")
    private Integer starLevel;

    @TableLogic
    private Integer deleted;
}
