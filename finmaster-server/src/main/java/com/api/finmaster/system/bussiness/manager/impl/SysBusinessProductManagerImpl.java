package com.qykj.finmaster.system.bussiness.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.bussiness.domain.SysBusinessProduct;
import com.qykj.finmaster.system.bussiness.manager.SysBusinessProductManager;
import com.qykj.finmaster.system.bussiness.mapper.SysBusinessProductMapper;
import org.springframework.stereotype.Service;

@Service
public class SysBusinessProductManagerImpl extends ManagerImpl<SysBusinessProductMapper, SysBusinessProduct> implements SysBusinessProductManager {
}

