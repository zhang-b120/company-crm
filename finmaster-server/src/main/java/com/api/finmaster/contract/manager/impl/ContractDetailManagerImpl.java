package com.qykj.finmaster.contract.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.contract.domain.ContractDetail;
import com.qykj.finmaster.contract.manager.ContractDetailManager;
import com.qykj.finmaster.contract.mapper.ContractDetailMapper;
import org.springframework.stereotype.Service;

/**
 * 合同明细Manager实现
 */
@Service
public class ContractDetailManagerImpl extends ManagerImpl<ContractDetailMapper, ContractDetail> implements ContractDetailManager {
}
