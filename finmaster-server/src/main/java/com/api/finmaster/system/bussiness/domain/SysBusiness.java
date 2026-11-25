package com.qykj.finmaster.system.bussiness.domain;

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
 * 业务信息对象 sys_business
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_business")
public class SysBusiness extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 4693425872309489213L;

    /**
     * 业务ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 业务编码
     */
    @TableField("business_code")
    private String businessCode;

    /**
     * 业务名称
     */
    @TableField("business_name")
    private String businessName;

    /**
     * 合同是否必须关联订单 0-否 1-是
     */
    @TableField("contract_related_order")
    private Integer contractRelatedOrder;

    /**
     * 判断转为续费订单次数阈值
     */
    @TableField("check_renewal_count")
    private Integer checkRenewalCount;

    /**
     * 存在时不可新增的业务ID，逗号分隔多选（如 2,3,5）
     */
    @TableField("check_add_business_id")
    private String checkAddBusinessId;

    /**
     * 是否删除（0-否 1-是）
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}

