package com.qykj.finmaster.sale.callRecord.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.sale.callRecord.entity.CallRecord;
import com.qykj.finmaster.sale.callRecord.manager.CallRecordManager;
import com.qykj.finmaster.sale.callRecord.mapper.CallRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class CallRecordManagerImpl extends ManagerImpl<CallRecordMapper, CallRecord> implements CallRecordManager {
}
