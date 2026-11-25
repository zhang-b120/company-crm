package com.qykj.finmaster.customer.company.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.company.domain.CompanyEmployee;
import com.qykj.finmaster.customer.company.manager.CompanyEmployeeManager;
import com.qykj.finmaster.customer.company.mapper.CompanyEmployeeMapper;
import org.springframework.stereotype.Service;

@Service
public class CompanyEmployeeManagerImpl
        extends ManagerImpl<CompanyEmployeeMapper, CompanyEmployee>
        implements CompanyEmployeeManager {
}
