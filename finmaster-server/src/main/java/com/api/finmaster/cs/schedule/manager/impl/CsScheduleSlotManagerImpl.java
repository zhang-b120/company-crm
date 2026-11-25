package com.qykj.finmaster.cs.schedule.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.cs.schedule.domain.CsScheduleSlot;
import com.qykj.finmaster.cs.schedule.manager.CsScheduleSlotManager;
import com.qykj.finmaster.cs.schedule.mapper.CsScheduleSlotMapper;
import org.springframework.stereotype.Component;

@Component
public class CsScheduleSlotManagerImpl extends ManagerImpl<CsScheduleSlotMapper, CsScheduleSlot>
        implements CsScheduleSlotManager {
}
