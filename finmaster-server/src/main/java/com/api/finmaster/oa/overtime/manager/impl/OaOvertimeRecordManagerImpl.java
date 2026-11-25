package com.qykj.finmaster.oa.overtime.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.overtime.domain.OaOvertimeRecord;
import com.qykj.finmaster.oa.overtime.manager.OaOvertimeRecordManager;
import com.qykj.finmaster.oa.overtime.mapper.OaOvertimeRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class OaOvertimeRecordManagerImpl extends ManagerImpl<OaOvertimeRecordMapper, OaOvertimeRecord> implements OaOvertimeRecordManager {
}
