package com.qykj.finmaster.customer.company.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.company.domain.CompanyLegalPerson;
import com.qykj.finmaster.customer.company.manager.CompanyLegalPersonManager;
import com.qykj.finmaster.customer.company.mapper.CompanyLegalPersonMapper;
import org.springframework.stereotype.Service;

@Service
public class CompanyLegalPersonManagerImpl
        extends ManagerImpl<CompanyLegalPersonMapper, CompanyLegalPerson>
        implements CompanyLegalPersonManager {
}
