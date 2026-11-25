package com.qykj.finmaster.customer.company.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.company.domain.CompanyContact;
import com.qykj.finmaster.customer.company.manager.CompanyContactManager;
import com.qykj.finmaster.customer.company.mapper.CompanyContactMapper;
import org.springframework.stereotype.Service;

@Service
public class CompanyContactManagerImpl
        extends ManagerImpl<CompanyContactMapper, CompanyContact>
        implements CompanyContactManager {
}
