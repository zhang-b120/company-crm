package com.qykj.finmaster.contract.domain;

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
 * 合同明细对象 contract_detail
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract_detail")
public class ContractDetail extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 合同详情ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 合同ID
     */
    @TableField("contract_id")
    private Long contractId;

    /**
     * 订单编号
     */
    @TableField("order_no")
    private String orderNo;

    /**
     * 订单明细ID
     */
    @TableField("order_detail_id")
    private Long orderDetailId;

    /**
     * 产品ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 注册地址ID
     */
    @TableField("company_address_id")
    private Long companyAddressId;

    /**
     * 是否删除（0-否 1-是）
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
