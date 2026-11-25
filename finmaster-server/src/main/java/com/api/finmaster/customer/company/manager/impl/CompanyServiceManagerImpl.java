package com.qykj.finmaster.customer.company.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.company.domain.CompanyService;
import com.qykj.finmaster.customer.company.manager.CompanyServiceManager;
import com.qykj.finmaster.customer.company.mapper.CompanyServiceMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@Service
public class CompanyServiceManagerImpl extends ManagerImpl<CompanyServiceMapper, CompanyService> implements CompanyServiceManager {
}