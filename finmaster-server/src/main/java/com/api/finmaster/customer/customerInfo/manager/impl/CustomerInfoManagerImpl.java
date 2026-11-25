package com.qykj.finmaster.customer.customerInfo.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.CustomerInfoManager;
import com.qykj.finmaster.customer.customerInfo.mapper.CustomerInfoMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-05-29
 * @Description
 */
@Service
public class CustomerInfoManagerImpl extends ManagerImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoManager {
}