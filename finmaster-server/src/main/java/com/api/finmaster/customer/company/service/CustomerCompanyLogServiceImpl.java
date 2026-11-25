package com.qykj.finmaster.customer.company.service;

import com.qykj.finmaster.customer.company.domain.CustomerCompanyLog;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyLogManager;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author zhb
 * @Date 2025-06-09
 * @Description
 */
@Service
public class CustomerCompanyLogServiceImpl {

    @Autowired
    private CustomerCompanyLogManager customerCompanyLogManager;

    @Transactional(rollbackFor = Exception.class)
    public Boolean addLog(Long customerId, String customerName, Integer logType, String content, Long tenantId){
        CustomerCompanyLog customerCompanyLog = new CustomerCompanyLog();
        customerCompanyLog.setLogType(logType);
        customerCompanyLog.setContent(content);
        customerCompanyLog.setTenantId(tenantId);
        if(ObjectUtils.isNotEmpty(customerId)){
            customerCompanyLog.setCustomerId(customerId);
            customerCompanyLog.setOperatorId(customerId);
            customerCompanyLog.setOperatorName(customerName);
            customerCompanyLog.setCreateBy(String.valueOf(customerId));
            customerCompanyLog.setCreateByName(customerName);
        }
        return customerCompanyLogManager.save(customerCompanyLog);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean addLog(Long customerId, String customerName, Integer logType, String content, Long operatorId, String operatorName, Long tenantId){
        CustomerCompanyLog customerCompanyLog = new CustomerCompanyLog();
        customerCompanyLog.setCustomerId(customerId);
        customerCompanyLog.setLogType(logType);
        customerCompanyLog.setContent(content);
        customerCompanyLog.setOperatorId(operatorId);
        customerCompanyLog.setOperatorName(operatorName);
        customerCompanyLog.setCreateBy(String.valueOf(customerId));
        customerCompanyLog.setCreateByName(customerName);
        customerCompanyLog.setTenantId(tenantId);
        return customerCompanyLogManager.save(customerCompanyLog);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean addLogByUpdate(Long customerId, Integer logType, String content, Long updateBy, String updateByName, Long tenantId){
        CustomerCompanyLog customerCompanyLog = new CustomerCompanyLog();
        customerCompanyLog.setCustomerId(customerId);
        customerCompanyLog.setLogType(logType);
        customerCompanyLog.setContent(content);
        customerCompanyLog.setOperatorId(updateBy);
        customerCompanyLog.setOperatorName(updateByName);
        customerCompanyLog.setUpdateBy(String.valueOf(updateBy));
        customerCompanyLog.setUpdateByName(updateByName);
        customerCompanyLog.setTenantId(tenantId);
        return customerCompanyLogManager.save(customerCompanyLog);
    }
}