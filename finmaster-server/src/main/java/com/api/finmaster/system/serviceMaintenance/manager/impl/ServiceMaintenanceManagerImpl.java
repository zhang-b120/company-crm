package com.qykj.finmaster.system.serviceMaintenance.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceMaintenance;
import com.qykj.finmaster.system.serviceMaintenance.manager.ServiceMaintenanceManager;
import com.qykj.finmaster.system.serviceMaintenance.mapper.ServiceMaintenanceMapper;
import org.springframework.stereotype.Service;

@Service
public class ServiceMaintenanceManagerImpl extends ManagerImpl<ServiceMaintenanceMapper, ServiceMaintenance> implements ServiceMaintenanceManager {
}
