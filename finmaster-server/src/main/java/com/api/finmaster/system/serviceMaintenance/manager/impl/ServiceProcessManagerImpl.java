package com.qykj.finmaster.system.serviceMaintenance.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceProcess;
import com.qykj.finmaster.system.serviceMaintenance.manager.ServiceProcessManager;
import com.qykj.finmaster.system.serviceMaintenance.mapper.ServiceProcessMapper;
import org.springframework.stereotype.Service;

@Service
public class ServiceProcessManagerImpl extends ManagerImpl<ServiceProcessMapper, ServiceProcess> implements ServiceProcessManager {
}
