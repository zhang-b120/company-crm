package com.qykj.finmaster.customer.company.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.customer.company.mapper.CustomerCompanyMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@Service
public class CustomerCompanyManagerImpl extends ManagerImpl<CustomerCompanyMapper, CustomerCompany> implements CustomerCompanyManager {

    @Override
    public List<Long> getCompanyIdsBySameTaxNumber(Long companyId, Long tenantId) {
        if (companyId == null) {
            return Collections.emptyList();
        }
        CustomerCompany company = getById(companyId);
        if (company == null) {
            return Collections.emptyList();
        }
        String taxNumber = company.getTaxNumber();
        if (StrUtil.isBlank(taxNumber)) {
            return Collections.singletonList(companyId);
        }
        Long tid = tenantId != null ? tenantId : company.getTenantId();
        List<CustomerCompany> list = lambdaQuery()
                .eq(tid != null, CustomerCompany::getTenantId, tid)
                .eq(CustomerCompany::getTaxNumber, taxNumber)
                .list();
        return list.stream().map(CustomerCompany::getId).filter(id -> id != null).collect(Collectors.toList());
    }
}