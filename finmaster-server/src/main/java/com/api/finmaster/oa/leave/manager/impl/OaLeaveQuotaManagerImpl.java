package com.qykj.finmaster.oa.leave.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.leave.domain.OaLeaveQuota;
import com.qykj.finmaster.oa.leave.manager.OaLeaveQuotaManager;
import com.qykj.finmaster.oa.leave.mapper.OaLeaveQuotaMapper;
import org.springframework.stereotype.Service;

@Service
public class OaLeaveQuotaManagerImpl extends ManagerImpl<OaLeaveQuotaMapper, OaLeaveQuota> implements OaLeaveQuotaManager {
}
