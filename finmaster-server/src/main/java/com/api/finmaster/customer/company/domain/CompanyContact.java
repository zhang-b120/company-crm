package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公司联系人 company_contact
 * 一个公司可以有多个联系人；客户主联系人仍走 customer_info，本表只承载公司级别的额外联系人
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_contact")
public class CompanyContact extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 公司ID */
    @TableField("company_id")
    private Long companyId;

    /** 姓名 */
    @TableField("name")
    private String name;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 职位 */
    @TableField("position")
    private String position;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 逻辑删除 0-未删 1-已删 */
    @TableLogic
    private Integer deleted;
}
