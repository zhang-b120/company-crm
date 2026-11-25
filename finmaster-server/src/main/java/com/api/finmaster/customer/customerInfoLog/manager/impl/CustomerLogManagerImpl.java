package com.qykj.finmaster.customer.customerInfoLog.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.customerInfoLog.domain.CustomerLog;
import com.qykj.finmaster.customer.customerInfoLog.manager.CustomerLogManager;
import com.qykj.finmaster.customer.customerInfoLog.mapper.CustomerLogMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@Service
public class CustomerLogManagerImpl extends ManagerImpl<CustomerLogMapper, CustomerLog> implements CustomerLogManager {
}