package com.qykj.finmaster.system.serviceMaintenance.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("service_maintenance_product")
public class ServiceMaintenanceProduct extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 2895663490670750008L;
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联产品信息ID
     */
    @TableField("service_id")
    private Long serviceId;

    /**
     * 关联产品信息ID
     */
    @TableField("product_id")
    private Long productId;

    /**
     * 关联产品信息名称
     */
    @TableField("product_name")
    private String productName;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;
}
