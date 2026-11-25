package com.qykj.finmaster.oa.leave.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.leave.domain.OaLeave;
import com.qykj.finmaster.oa.leave.manager.OaLeaveManager;
import com.qykj.finmaster.oa.leave.mapper.OaLeaveMapper;
import org.springframework.stereotype.Service;

/**
 * 请假Manager实现
 */
@Service
public class OaLeaveManagerImpl extends ManagerImpl<OaLeaveMapper, OaLeave> implements OaLeaveManager {
}
