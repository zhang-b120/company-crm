package com.qykj.finmaster.workorder.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.workorder.domain.WorkOrderFieldRecord;
import com.qykj.finmaster.workorder.manager.WorkOrderFieldRecordManager;
import com.qykj.finmaster.workorder.mapper.WorkOrderFieldRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderFieldRecordManagerImpl extends ManagerImpl<WorkOrderFieldRecordMapper, WorkOrderFieldRecord> implements WorkOrderFieldRecordManager {
}
