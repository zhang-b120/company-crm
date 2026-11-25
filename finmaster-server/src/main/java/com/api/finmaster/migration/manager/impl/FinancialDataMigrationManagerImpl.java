package com.qykj.finmaster.migration.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.migration.domain.FinancialDataMigration;
import com.qykj.finmaster.migration.manager.FinancialDataMigrationManager;
import com.qykj.finmaster.migration.mapper.FinancialDataMigrationMapper;
import org.springframework.stereotype.Service;

/**
 * 财务数据迁移Manager实现类
 * @Author zhb
 * @Date 2025-07-14
 * @Description
 */
@Service
public class FinancialDataMigrationManagerImpl extends ManagerImpl<FinancialDataMigrationMapper, FinancialDataMigration> implements FinancialDataMigrationManager {
} 