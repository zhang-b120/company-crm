package com.qykj.finmaster.system.serviceMaintenance.domain.resp;

import com.qykj.finmaster.system.serviceMaintenance.domain.dto.ServiceProductInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class GetServiceMaintenanceInfoResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -195818597013802053L;
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 业务ID列表
     */
    private java.util.List<Long> businessIds;

    /**
     * 业务名称（多个以逗号拼接）
     */
    private String businessName;

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
     * 服务公共字段
     */
    private String maintenanceFiled;

    /**
     * 关联产品信息
     */
    private List<ServiceProductInfo> productInfoList;
}
