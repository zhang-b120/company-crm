package com.qykj.finmaster.workorder.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.workorder.domain.WorkOrderRoleConfig;
import com.qykj.finmaster.workorder.manager.WorkOrderRoleConfigManager;
import com.qykj.finmaster.workorder.mapper.WorkOrderRoleConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderRoleConfigManagerImpl extends ManagerImpl<WorkOrderRoleConfigMapper, WorkOrderRoleConfig> implements WorkOrderRoleConfigManager {
}
