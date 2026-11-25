package com.qykj.finmaster.workorder.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.workorder.domain.WorkOrderStep;
import com.qykj.finmaster.workorder.manager.WorkOrderStepManager;
import com.qykj.finmaster.workorder.mapper.WorkOrderStepMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderStepManagerImpl extends ManagerImpl<WorkOrderStepMapper, WorkOrderStep> implements WorkOrderStepManager {
}
