package com.qykj.finmaster.cs.profile.service;

import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.cs.profile.domain.FinancialTaxOfficer;
import com.qykj.finmaster.cs.profile.domain.req.SaveTaxOfficerReq;
import com.qykj.finmaster.cs.profile.mapper.FinancialProfileMapper;
import com.qykj.finmaster.cs.profile.mapper.FinancialTaxOfficerMapper;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CsTaxOfficerServiceImpl {

    @Autowired
    private FinancialTaxOfficerMapper taxOfficerMapper;
    @Autowired
    private FinancialProfileMapper financialProfileMapper;
    @Autowired
    private CustomerCompanyManagerImpl companyManager;

    /** 查询公司的办税员列表 */
    public List<FinancialTaxOfficer> listByCompanyId(Long companyId) {
        Long accountSetId = getAccountSetId(companyId);
        if (accountSetId == null) return List.of();
        return financialProfileMapper.getTaxOfficersByAccountSetId(accountSetId);
    }

    /** 新增/编辑办税员 */
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveTaxOfficer(SaveTaxOfficerReq req) {
        Long accountSetId = getAccountSetId(req.getCompanyId());
        if (accountSetId == null) throw new BusinessException("该公司在 financial 系统中未找到对应账套，请先在财务系统中建账");

        FinancialTaxOfficer officer;
        if (req.getId() != null) {
            officer = taxOfficerMapper.selectById(req.getId());
            if (officer == null) throw new BusinessException("办税员不存在");
            officer.setUpdatedAt(LocalDateTime.now());
        } else {
            officer = new FinancialTaxOfficer();
            officer.setAccountSetId(accountSetId);
            officer.setCreatedAt(LocalDateTime.now());
            officer.setUpdatedAt(LocalDateTime.now());
        }

        officer.setName(req.getName());
        officer.setPhone(req.getPhone());
        officer.setPassword(req.getPassword());
        officer.setLoginType(StringUtils.defaultIfBlank(req.getLoginType(), "officer"));
        officer.setIdCard(req.getIdCard());
        officer.setIsDefault(req.getIsDefault() != null && req.getIsDefault());
        officer.setRemark(req.getRemark());

        // 如果设为默认，先清除同账套其他默认标记
        if (Boolean.TRUE.equals(officer.getIsDefault())) {
            taxOfficerMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<FinancialTaxOfficer>()
                            .eq(FinancialTaxOfficer::getAccountSetId, accountSetId)
                            .set(FinancialTaxOfficer::getIsDefault, false));
        }

        if (req.getId() != null) {
            taxOfficerMapper.updateById(officer);
        } else {
            taxOfficerMapper.insert(officer);
        }
        return true;
    }

    /** 删除办税员 */
    public Boolean deleteTaxOfficer(Long id) {
        return taxOfficerMapper.deleteById(id) > 0;
    }

    /** 根据 companyId 查询 financial 账套ID */
    private Long getAccountSetId(Long companyId) {
        CustomerCompany company = companyManager.getById(companyId);
        if (company == null || StringUtils.isBlank(company.getTaxNumber())) return null;
        return financialProfileMapper.getAccountSetIdByTaxNumber(company.getTaxNumber());
    }
}
