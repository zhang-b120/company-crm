package com.qykj.finmaster.contract.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.manager.ContractManager;
import com.qykj.finmaster.contract.mapper.ContractMapper;
import org.springframework.stereotype.Service;

/**
 * 合同Manager实现
 */
@Service
public class ContractManagerImpl extends ManagerImpl<ContractMapper, Contract> implements ContractManager {
}
