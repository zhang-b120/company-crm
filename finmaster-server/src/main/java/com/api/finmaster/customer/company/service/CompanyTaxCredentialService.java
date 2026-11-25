package com.qykj.finmaster.customer.company.service;

import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.customer.company.domain.CompanyTaxCredential;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.domain.req.SaveCompanyTaxCredentialReq;
import com.qykj.finmaster.customer.company.domain.req.UaGetCompanyTaxCredentialReq;
import com.qykj.finmaster.customer.company.domain.req.UaSaveCompanyTaxCredentialReq;
import com.qykj.finmaster.customer.company.manager.CompanyTaxCredentialManager;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.security.LoginUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CompanyTaxCredentialService {

    @Autowired
    private CompanyTaxCredentialManager manager;

    @Autowired
    private CustomerCompanyManager customerCompanyManager;

    public CompanyTaxCredential getByCompanyId(Long companyId) {
        return manager.lambdaQuery()
                .eq(CompanyTaxCredential::getCompanyId, companyId)
                .one();
    }

    /** 存在则更新，不存在则新增（upsert） */
    public CompanyTaxCredential save(SaveCompanyTaxCredentialReq req, LoginUser loginUser) {
        CompanyTaxCredential existing = getByCompanyId(req.getCompanyId());
        Date now = new Date();
        if (existing != null) {
            existing.setIndividualTaxPassword(req.getIndividualTaxPassword());
            existing.setIndividualTaxAppDownloaded(req.getIndividualTaxAppDownloaded());
            existing.setETaxPassword(req.getETaxPassword());
            existing.setETaxAppDownloaded(req.getETaxAppDownloaded());
            existing.setTaxOfficerConfigured(req.getTaxOfficerConfigured());
            existing.setAgentChannelConfigured(req.getAgentChannelConfigured());
            existing.setUpdateBy(loginUser.getUsername());
            existing.setUpdateByName(loginUser.getRealName());
            existing.setUpdateTime(now);
            manager.updateById(existing);
            return existing;
        }
        CompanyTaxCredential entity = new CompanyTaxCredential();
        entity.setCompanyId(req.getCompanyId());
        entity.setIndividualTaxPassword(req.getIndividualTaxPassword());
        entity.setIndividualTaxAppDownloaded(req.getIndividualTaxAppDownloaded());
        entity.setETaxPassword(req.getETaxPassword());
        entity.setETaxAppDownloaded(req.getETaxAppDownloaded());
        entity.setTaxOfficerConfigured(req.getTaxOfficerConfigured());
        entity.setAgentChannelConfigured(req.getAgentChannelConfigured());
        entity.setCreateBy(loginUser.getUsername());
        entity.setCreateByName(loginUser.getRealName());
        entity.setCreateTime(now);
        entity.setUpdateBy(loginUser.getUsername());
        entity.setUpdateByName(loginUser.getRealName());
        entity.setUpdateTime(now);
        manager.save(entity);
        return entity;
    }

    /* ================= 小程序（UA）接口 ================= */

    /**
     * 小程序读取凭证：以 openId 校验 companyId 归属
     */
    public CompanyTaxCredential uaGet(UaGetCompanyTaxCredentialReq req) {
        checkOwnership(req.getOpenId(), req.getCompanyId());
        return getByCompanyId(req.getCompanyId());
    }

    /**
     * 小程序保存凭证：以 openId 校验 companyId 归属，然后 upsert
     */
    public CompanyTaxCredential uaSave(UaSaveCompanyTaxCredentialReq req) {
        checkOwnership(req.getOpenId(), req.getCompanyId());
        CompanyTaxCredential existing = getByCompanyId(req.getCompanyId());
        Date now = new Date();
        if (existing != null) {
            existing.setIndividualTaxPassword(req.getIndividualTaxPassword());
            existing.setIndividualTaxAppDownloaded(req.getIndividualTaxAppDownloaded());
            existing.setETaxPassword(req.getETaxPassword());
            existing.setETaxAppDownloaded(req.getETaxAppDownloaded());
            existing.setTaxOfficerConfigured(req.getTaxOfficerConfigured());
            existing.setAgentChannelConfigured(req.getAgentChannelConfigured());
            existing.setUpdateBy(req.getOpenId());
            existing.setUpdateByName("小程序用户");
            existing.setUpdateTime(now);
            manager.updateById(existing);
            return existing;
        }
        CompanyTaxCredential entity = new CompanyTaxCredential();
        entity.setCompanyId(req.getCompanyId());
        entity.setIndividualTaxPassword(req.getIndividualTaxPassword());
        entity.setIndividualTaxAppDownloaded(req.getIndividualTaxAppDownloaded());
        entity.setETaxPassword(req.getETaxPassword());
        entity.setETaxAppDownloaded(req.getETaxAppDownloaded());
        entity.setTaxOfficerConfigured(req.getTaxOfficerConfigured());
        entity.setAgentChannelConfigured(req.getAgentChannelConfigured());
        entity.setCreateBy(req.getOpenId());
        entity.setCreateByName("小程序用户");
        entity.setCreateTime(now);
        entity.setUpdateBy(req.getOpenId());
        entity.setUpdateByName("小程序用户");
        entity.setUpdateTime(now);
        manager.save(entity);
        return entity;
    }

    /**
     * 校验 openId 与 companyId 归属关系：customer_company 中必须存在
     * 一条 openid = openId 且 id = companyId 的记录
     */
    private void checkOwnership(String openId, Long companyId) {
        if (StringUtils.isBlank(openId) || companyId == null) {
            throw new BusinessException("参数不完整");
        }
        CustomerCompany cc = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .eq(CustomerCompany::getOpenid, openId)
                .one();
        if (cc == null) {
            throw new BusinessException("无权限操作该公司凭证");
        }
    }
}
