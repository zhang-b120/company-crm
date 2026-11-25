package com.qykj.finmaster.oa.approval.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.approval.domain.OaApprovalDelegation;
import com.qykj.finmaster.oa.approval.manager.OaApprovalDelegationManager;
import com.qykj.finmaster.oa.approval.mapper.OaApprovalDelegationMapper;
import org.springframework.stereotype.Service;

/**
 * 审批委托Manager实现
 */
@Service
public class OaApprovalDelegationManagerImpl extends ManagerImpl<OaApprovalDelegationMapper, OaApprovalDelegation> implements OaApprovalDelegationManager {
}
