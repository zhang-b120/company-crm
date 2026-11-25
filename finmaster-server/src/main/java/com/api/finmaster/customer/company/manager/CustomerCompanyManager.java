package com.qykj.finmaster.customer.company.manager;

import com.qykj.finmaster.common.base.IManager;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.mapper.CustomerCompanyMapper;

import java.util.List;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
public interface CustomerCompanyManager extends IManager<CustomerCompanyMapper, CustomerCompany> {

    /**
     * 根据公司ID与租户ID，返回同税号（同一公司）的所有公司ID列表。
     *
     * @param companyId 公司ID
     * @param tenantId  租户ID
     * @return 同税号的公司ID列表（含自身）
     */
    List<Long> getCompanyIdsBySameTaxNumber(Long companyId, Long tenantId);
}