package com.qykj.finmaster.system.serviceMaintenance.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceMaintenanceProcess;
import com.qykj.finmaster.system.serviceMaintenance.manager.ServiceMaintenanceProcessManager;
import com.qykj.finmaster.system.serviceMaintenance.mapper.ServiceMaintenanceProcessMapper;
import org.springframework.stereotype.Service;

@Service
public class ServiceMaintenanceProcessManagerImpl extends ManagerImpl<ServiceMaintenanceProcessMapper, ServiceMaintenanceProcess> implements ServiceMaintenanceProcessManager {
}
