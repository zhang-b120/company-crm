package com.qykj.finmaster.cs.schedule.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.cs.schedule.domain.CsScheduleConfig;
import com.qykj.finmaster.cs.schedule.manager.CsScheduleConfigManager;
import com.qykj.finmaster.cs.schedule.mapper.CsScheduleConfigMapper;
import org.springframework.stereotype.Component;

@Component
public class CsScheduleConfigManagerImpl extends ManagerImpl<CsScheduleConfigMapper, CsScheduleConfig>
        implements CsScheduleConfigManager {
}
