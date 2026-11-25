package com.qykj.finmaster.oa.approval.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.approval.domain.ApprovalDefinition;
import com.qykj.finmaster.oa.approval.manager.ApprovalDefinitionManager;
import com.qykj.finmaster.oa.approval.mapper.ApprovalDefinitionMapper;
import org.springframework.stereotype.Service;

/**
 * 审批流程定义Manager实现
 */
@Service
public class ApprovalDefinitionManagerImpl extends ManagerImpl<ApprovalDefinitionMapper, ApprovalDefinition> implements ApprovalDefinitionManager {
}
