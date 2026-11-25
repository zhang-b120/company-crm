package com.qykj.finmaster.system.serviceMaintenance.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceMaintenanceProduct;
import com.qykj.finmaster.system.serviceMaintenance.manager.ServiceMaintenanceProductManager;
import com.qykj.finmaster.system.serviceMaintenance.mapper.ServiceMaintenanceProductMapper;
import org.springframework.stereotype.Service;

@Service
public class ServiceMaintenanceProductManagerImpl extends ManagerImpl<ServiceMaintenanceProductMapper, ServiceMaintenanceProduct> implements ServiceMaintenanceProductManager {
}
