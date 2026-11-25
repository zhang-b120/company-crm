package com.qykj.finmaster.workorder.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.workorder.domain.WorkOrderAttachment;
import com.qykj.finmaster.workorder.manager.WorkOrderAttachmentManager;
import com.qykj.finmaster.workorder.mapper.WorkOrderAttachmentMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderAttachmentManagerImpl extends ManagerImpl<WorkOrderAttachmentMapper, WorkOrderAttachment> implements WorkOrderAttachmentManager {
}
