package com.qykj.finmaster.system.bussiness.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.bussiness.domain.SysBusiness;
import com.qykj.finmaster.system.bussiness.manager.SysBusinessManager;
import com.qykj.finmaster.system.bussiness.mapper.SysBusinessMapper;
import org.springframework.stereotype.Service;

@Service
public class SysBusinessManagerImpl extends ManagerImpl<SysBusinessMapper, SysBusiness> implements SysBusinessManager {
}

