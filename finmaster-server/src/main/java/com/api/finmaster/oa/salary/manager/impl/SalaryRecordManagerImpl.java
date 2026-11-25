package com.qykj.finmaster.oa.salary.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.salary.domain.SalaryRecord;
import com.qykj.finmaster.oa.salary.manager.SalaryRecordManager;
import com.qykj.finmaster.oa.salary.mapper.SalaryRecordMapper;
import org.springframework.stereotype.Service;

/**
 * 薪资记录Manager实现
 */
@Service
public class SalaryRecordManagerImpl extends ManagerImpl<SalaryRecordMapper, SalaryRecord> implements SalaryRecordManager {
}
