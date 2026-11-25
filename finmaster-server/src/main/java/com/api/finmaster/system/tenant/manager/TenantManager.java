package com.qykj.finmaster.system.tenant.manager;

import com.qykj.finmaster.common.base.IManager;
import com.qykj.finmaster.system.tenant.entity.Tenant;
import com.qykj.finmaster.system.tenant.mapper.TenantMapper;

/**
 * 用户Manager接口
 * 
 * @Author zhb
 * @Date 2025-01-03
 * @Description 用户管理数据访问层
 */
public interface TenantManager extends IManager<TenantMapper, Tenant> {
    
} 