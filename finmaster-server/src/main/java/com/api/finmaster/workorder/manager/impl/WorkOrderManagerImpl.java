package com.qykj.finmaster.workorder.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.workorder.domain.WorkOrder;
import com.qykj.finmaster.workorder.manager.WorkOrderManager;
import com.qykj.finmaster.workorder.mapper.WorkOrderMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderManagerImpl extends ManagerImpl<WorkOrderMapper, WorkOrder> implements WorkOrderManager {
}
