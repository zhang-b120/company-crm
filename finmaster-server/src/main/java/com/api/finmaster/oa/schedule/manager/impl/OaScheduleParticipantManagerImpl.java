package com.qykj.finmaster.oa.schedule.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.schedule.domain.OaScheduleParticipant;
import com.qykj.finmaster.oa.schedule.manager.OaScheduleParticipantManager;
import com.qykj.finmaster.oa.schedule.mapper.OaScheduleParticipantMapper;
import org.springframework.stereotype.Service;

/**
 * 日程参与人Manager实现
 */
@Service
public class OaScheduleParticipantManagerImpl extends ManagerImpl<OaScheduleParticipantMapper, OaScheduleParticipant> implements OaScheduleParticipantManager {
}
