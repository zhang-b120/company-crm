package com.qykj.finmaster.oa.approval.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.approval.domain.OaApprovalReminder;
import com.qykj.finmaster.oa.approval.manager.OaApprovalReminderManager;
import com.qykj.finmaster.oa.approval.mapper.OaApprovalReminderMapper;
import org.springframework.stereotype.Service;

/**
 * 审批催办记录Manager实现
 */
@Service
public class OaApprovalReminderManagerImpl extends ManagerImpl<OaApprovalReminderMapper, OaApprovalReminder> implements OaApprovalReminderManager {
}
