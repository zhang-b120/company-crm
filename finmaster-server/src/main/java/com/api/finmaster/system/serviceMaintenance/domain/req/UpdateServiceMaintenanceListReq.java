package com.qykj.finmaster.system.serviceMaintenance.domain.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.qykj.finmaster.system.serviceMaintenance.domain.dto.ServiceProductInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class UpdateServiceMaintenanceListReq implements Serializable {
    private static final long serialVersionUID = -3876025033274269357L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

    /**
     * 是否跨租户
     */
    private Boolean crossTenantIsAllowed;

    /**
     * 服务描述
     */
    private String description;

    /**
     * 业务ID列表（一个服务可关联多个业务）
     */
    private java.util.List<Long> businessIds;
}
