package com.qykj.finmaster.oa.approval.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.approval.domain.ApprovalInstance;
import com.qykj.finmaster.oa.approval.manager.ApprovalInstanceManager;
import com.qykj.finmaster.oa.approval.mapper.ApprovalInstanceMapper;
import org.springframework.stereotype.Service;

/**
 * 审批实例Manager实现
 */
@Service
public class ApprovalInstanceManagerImpl extends ManagerImpl<ApprovalInstanceMapper, ApprovalInstance> implements ApprovalInstanceManager {
}
