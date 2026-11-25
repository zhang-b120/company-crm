package com.qykj.finmaster.oa.schedule.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.schedule.domain.OaSchedule;
import com.qykj.finmaster.oa.schedule.manager.OaScheduleManager;
import com.qykj.finmaster.oa.schedule.mapper.OaScheduleMapper;
import org.springframework.stereotype.Service;

/**
 * 日程Manager实现
 */
@Service
public class OaScheduleManagerImpl extends ManagerImpl<OaScheduleMapper, OaSchedule> implements OaScheduleManager {
}
