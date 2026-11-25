package com.qykj.finmaster.customer.company.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.company.domain.CustomerCompanyLog;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyLogManager;
import com.qykj.finmaster.customer.company.mapper.CustomerCompanyLogMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-06-09
 * @Description
 */
@Service
public class CustomerCompanyLogManagerImpl extends ManagerImpl<CustomerCompanyLogMapper, CustomerCompanyLog> implements CustomerCompanyLogManager {
}