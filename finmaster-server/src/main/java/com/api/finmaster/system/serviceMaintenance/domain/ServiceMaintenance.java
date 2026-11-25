package com.qykj.finmaster.system.serviceMaintenance.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("service_maintenance")
public class ServiceMaintenance extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 2895663490670750008L;
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 服务名称
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 负责人
     */
    @TableField("owner_user")
    private String ownerUser;

    /**
     * 负责人名称
     */
    @TableField("owner_user_name")
    private String ownerUserName;

    /**
     * 是否跨租户
     */
    @TableField("cross_tenantis_allowed")
    private Boolean crossTenantIsAllowed;

    /**
     * 服务描述
     */
    @TableField("description")
    private String description;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 服务公共字段
     */
    @TableField("maintenance_filed")
    private String maintenanceFiled;
}
