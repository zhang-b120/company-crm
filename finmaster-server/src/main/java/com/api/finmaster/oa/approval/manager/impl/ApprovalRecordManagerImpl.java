package com.qykj.finmaster.oa.approval.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.approval.domain.ApprovalRecord;
import com.qykj.finmaster.oa.approval.manager.ApprovalRecordManager;
import com.qykj.finmaster.oa.approval.mapper.ApprovalRecordMapper;
import org.springframework.stereotype.Service;

/**
 * 审批记录Manager实现
 */
@Service
public class ApprovalRecordManagerImpl extends ManagerImpl<ApprovalRecordMapper, ApprovalRecord> implements ApprovalRecordManager {
}
