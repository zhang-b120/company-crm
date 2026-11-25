package com.qykj.finmaster.migration.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.common.utils.third.req.AddAccountSetMigrationDataReq;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.migration.domain.FinancialDataMigration;
import com.qykj.finmaster.migration.manager.impl.FinancialDataMigrationManagerImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 财务数据迁移服务实现类
 * @Author zhb
 * @Date 2025-07-14
 * @Description
 */
@Service
public class FinancialDataMigrationServiceImpl{

    @Autowired
    private FinancialDataMigrationManagerImpl financialDataMigrationManager;
    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;

    /**
     * 数据迁移
     * @param companyId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean migrationData(Long companyId) {
        if(ObjectUtils.isEmpty(companyId)){
            throw new BusinessException(ResultMsg.MIGRATION_NOT_COMPANY_ID);
        }
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        if(ObjectUtils.isEmpty(customerCompany)){
            throw new BusinessException(ResultMsg.MIGRATION_NOT_COMPANY_DATA);
        }
        if(ObjectUtils.isEmpty(customerCompany.getExperienceAccountSetId())){
            throw new BusinessException(ResultMsg.MIGRATION_NOT_EXPERIENCE);
        }
        //获取体验账套数据
        List<FinancialDataMigration> list = financialDataMigrationManager.lambdaQuery()
                .eq(FinancialDataMigration::getExperienceAccountSetId, customerCompany.getExperienceAccountSetId())
                .eq(FinancialDataMigration::getMigrationStatus, GlobalConstant.Number.ONE_INT)
                .list();
        if(CollUtil.isNotEmpty(list)){
            throw new BusinessException(ResultMsg.MIGRATION_DATA_REPEAT);
        }
        JSONArray experienceMigrationData = FinancialUtils.getExperienceMigrationData(customerCompany.getExperienceAccountSetId());
        if (CollUtil.isNotEmpty(experienceMigrationData)) {
            list = experienceMigrationData.stream().map(item -> {
                FinancialDataMigration data = CopierCopyUtils.convertTo(item, FinancialDataMigration.class);
                data.setMigrationStatus(GlobalConstant.Number.ZERO_INT);
                return data;
            }).toList();
            financialDataMigrationManager.saveBatch(list);
        }
        //添加至正式账套
        if(CollUtil.isNotEmpty(list)){
            JSONObject dataJson = FinancialUtils.getAccountSets(customerCompany.getTaxNumber());
            if(ObjectUtils.isNotEmpty(dataJson)){
                Long accountSetId = Long.valueOf(dataJson.getLong("id").toString());
                list.forEach(item -> item.setAccountSetId(accountSetId));
                Boolean flag = FinancialUtils.addAccountSetMigrationData(CopierCopyUtils.convertTo(list, AddAccountSetMigrationDataReq.class));
                if(!flag){
                    throw new BusinessException(ResultMsg.MIGRATION_DATA_FAIL);
                }else {
                    list.forEach(item -> item.setMigrationStatus(GlobalConstant.Number.ONE_INT));
                    financialDataMigrationManager.updateBatchById(list);
                }
            }
        }else{
            throw new BusinessException(ResultMsg.MIGRATION_NOT_DATA);
        }
        return Boolean.TRUE;
    }
}