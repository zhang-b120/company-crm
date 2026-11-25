package com.qykj.finmaster.cs.profile.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.cs.profile.domain.CsCommunicationRecord;
import com.qykj.finmaster.cs.profile.manager.CsCommunicationRecordManager;
import com.qykj.finmaster.cs.profile.mapper.CsCommunicationRecordMapper;
import org.springframework.stereotype.Component;

@Component
public class CsCommunicationRecordManagerImpl extends ManagerImpl<CsCommunicationRecordMapper, CsCommunicationRecord>
        implements CsCommunicationRecordManager {
}
