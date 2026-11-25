package com.qykj.finmaster.oa.salary.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.salary.domain.SalaryConfig;
import com.qykj.finmaster.oa.salary.manager.SalaryConfigManager;
import com.qykj.finmaster.oa.salary.mapper.SalaryConfigMapper;
import org.springframework.stereotype.Service;

/**
 * 薪资配置Manager实现
 */
@Service
public class SalaryConfigManagerImpl extends ManagerImpl<SalaryConfigMapper, SalaryConfig> implements SalaryConfigManager {
}
