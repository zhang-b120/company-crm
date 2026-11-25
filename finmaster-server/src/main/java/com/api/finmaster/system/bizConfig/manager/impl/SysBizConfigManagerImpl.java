package com.qykj.finmaster.system.bizConfig.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.bizConfig.entity.SysBizConfig;
import com.qykj.finmaster.system.bizConfig.manager.SysBizConfigManager;
import com.qykj.finmaster.system.bizConfig.mapper.SysBizConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class SysBizConfigManagerImpl extends ManagerImpl<SysBizConfigMapper, SysBizConfig> implements SysBizConfigManager {
}
