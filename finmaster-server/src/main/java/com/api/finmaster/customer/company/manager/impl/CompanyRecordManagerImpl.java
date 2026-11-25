package com.qykj.finmaster.customer.company.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.company.domain.CompanyRecord;
import com.qykj.finmaster.customer.company.manager.CompanyRecordManager;
import com.qykj.finmaster.customer.company.mapper.CompanyRecordMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@Service
public class CompanyRecordManagerImpl extends ManagerImpl<CompanyRecordMapper, CompanyRecord> implements CompanyRecordManager {
}