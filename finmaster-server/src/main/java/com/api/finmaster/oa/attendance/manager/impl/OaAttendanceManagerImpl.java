package com.qykj.finmaster.oa.attendance.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.attendance.domain.OaAttendance;
import com.qykj.finmaster.oa.attendance.manager.OaAttendanceManager;
import com.qykj.finmaster.oa.attendance.mapper.OaAttendanceMapper;
import org.springframework.stereotype.Service;

/**
 * 考勤Manager实现
 */
@Service
public class OaAttendanceManagerImpl extends ManagerImpl<OaAttendanceMapper, OaAttendance> implements OaAttendanceManager {
}
