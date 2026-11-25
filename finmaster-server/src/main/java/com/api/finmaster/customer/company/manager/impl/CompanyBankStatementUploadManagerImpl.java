package com.qykj.finmaster.customer.company.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.company.domain.CompanyBankStatementUpload;
import com.qykj.finmaster.customer.company.manager.CompanyBankStatementUploadManager;
import com.qykj.finmaster.customer.company.mapper.CompanyBankStatementUploadMapper;
import org.springframework.stereotype.Service;

@Service
public class CompanyBankStatementUploadManagerImpl
        extends ManagerImpl<CompanyBankStatementUploadMapper, CompanyBankStatementUpload>
        implements CompanyBankStatementUploadManager {
}
