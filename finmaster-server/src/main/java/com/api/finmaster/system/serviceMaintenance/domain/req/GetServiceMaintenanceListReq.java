package com.qykj.finmaster.system.serviceMaintenance.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;
@Data
public class GetServiceMaintenanceListReq extends PageEntity{
    @Serial
    private static final long serialVersionUID = 2994906680455741767L;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 负责人名称
     */
    private String ownerUserName;

    /**
     * 业务类型ID（按业务筛选服务，用于合同转任务单等）
     */
    private Long businessId;
}
