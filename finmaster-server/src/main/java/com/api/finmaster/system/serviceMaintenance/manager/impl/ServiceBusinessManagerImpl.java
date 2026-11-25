package com.qykj.finmaster.system.serviceMaintenance.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceBusiness;
import com.qykj.finmaster.system.serviceMaintenance.manager.ServiceBusinessManager;
import com.qykj.finmaster.system.serviceMaintenance.mapper.ServiceBusinessMapper;
import org.springframework.stereotype.Service;

@Service
public class ServiceBusinessManagerImpl extends ManagerImpl<ServiceBusinessMapper, ServiceBusiness> implements ServiceBusinessManager {
}
