package com.qykj.finmaster.workorder.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.workorder.domain.WorkOrderLog;
import com.qykj.finmaster.workorder.manager.WorkOrderLogManager;
import com.qykj.finmaster.workorder.mapper.WorkOrderLogMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderLogManagerImpl extends ManagerImpl<WorkOrderLogMapper, WorkOrderLog> implements WorkOrderLogManager {
}
