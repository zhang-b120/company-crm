package com.qykj.finmaster.system.tenant.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.tenant.entity.Tenant;
import com.qykj.finmaster.system.tenant.manager.TenantManager;
import com.qykj.finmaster.system.tenant.mapper.TenantMapper;
import org.springframework.stereotype.Service;

/**
 * 用户Manager实现类
 * 
 * @Author zhb
 * @Date 2025-01-03
 * @Description 用户管理数据访问层实现
 */
@Service
public class TenantManagerImpl extends ManagerImpl<TenantMapper, Tenant> implements TenantManager {
    
} 