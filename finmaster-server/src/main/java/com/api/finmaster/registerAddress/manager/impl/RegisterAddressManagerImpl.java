package com.qykj.finmaster.registerAddress.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.registerAddress.domain.RegisterAddress;
import com.qykj.finmaster.registerAddress.manager.RegisterAddressManager;
import com.qykj.finmaster.registerAddress.mapper.RegisterAddressMapper;
import org.springframework.stereotype.Service;

/**
 * 注册地址Manager实现
 */
@Service
public class RegisterAddressManagerImpl extends ManagerImpl<RegisterAddressMapper, RegisterAddress> implements RegisterAddressManager {
}
