package com.qykj.finmaster.migration.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.migration.domain.FinancialDataMigration;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务数据迁移数据访问层
 * @Author zhb
 * @Date 2025-07-14
 * @Description
 */
@Mapper
public interface FinancialDataMigrationMapper extends BaseMapper<FinancialDataMigration> {

} 