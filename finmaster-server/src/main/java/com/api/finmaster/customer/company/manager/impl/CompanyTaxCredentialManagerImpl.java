package com.qykj.finmaster.customer.company.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.company.domain.CompanyTaxCredential;
import com.qykj.finmaster.customer.company.manager.CompanyTaxCredentialManager;
import com.qykj.finmaster.customer.company.mapper.CompanyTaxCredentialMapper;
import org.springframework.stereotype.Service;

@Service
public class CompanyTaxCredentialManagerImpl
        extends ManagerImpl<CompanyTaxCredentialMapper, CompanyTaxCredential>
        implements CompanyTaxCredentialManager {
}
