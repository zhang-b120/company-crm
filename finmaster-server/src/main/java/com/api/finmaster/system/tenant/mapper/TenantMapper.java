package com.qykj.finmaster.system.tenant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.system.tenant.entity.Tenant;
import com.qykj.finmaster.system.tenant.entity.resp.GetTenantPageResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 租户Mapper接口
 */
@Mapper
public interface TenantMapper extends BaseMapper<Tenant> {
    
    /**
     * 分页查询租户列表
     * @param page 分页参数
     * @param tenant 查询条件
     * @return 分页结果
     */
    List<GetTenantPageResp> selectTenantPage(@Param("params") Map<String, Object> params);
    
    /**
     * 根据租户编码查询租户
     * @param tenantCode 租户编码
     * @return 租户信息
     */
    Tenant selectByTenantCode(@Param("tenantCode") String tenantCode);
    
    /**
     * 根据租户编码查询租户（排除指定ID）
     * @param tenantCode 租户编码
     * @param id 排除的ID
     * @return 租户信息
     */
    Tenant selectByTenantCodeExcludeId(@Param("tenantCode") String tenantCode, @Param("id") Long id);
} 