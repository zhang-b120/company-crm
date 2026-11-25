package com.qykj.finmaster.oa.approval.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.approval.domain.OaApprovalCC;
import com.qykj.finmaster.oa.approval.manager.OaApprovalCCManager;
import com.qykj.finmaster.oa.approval.mapper.OaApprovalCCMapper;
import org.springframework.stereotype.Service;

/**
 * 审批抄送记录Manager实现
 */
@Service
public class OaApprovalCCManagerImpl extends ManagerImpl<OaApprovalCCMapper, OaApprovalCC> implements OaApprovalCCManager {
}
