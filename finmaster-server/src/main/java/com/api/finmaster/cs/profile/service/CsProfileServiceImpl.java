package com.qykj.finmaster.cs.profile.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.cs.profile.domain.*;
import com.qykj.finmaster.cs.profile.domain.req.AddCommunicationReq;
import com.qykj.finmaster.cs.profile.domain.req.GetProfilePageReq;
import com.qykj.finmaster.cs.profile.domain.req.SaveProfileReq;
import com.qykj.finmaster.cs.profile.domain.resp.ProfilePageResp;
import com.qykj.finmaster.cs.profile.manager.impl.CsCommunicationRecordManagerImpl;
import com.qykj.finmaster.cs.profile.manager.impl.CsCustomerProfileManagerImpl;
import com.qykj.finmaster.cs.profile.mapper.CsProfileMapper;
import com.qykj.finmaster.cs.profile.mapper.FinancialProfileMapper;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CsProfileServiceImpl {

    @Autowired
    private CsCustomerProfileManagerImpl profileManager;
    @Autowired
    private CsCommunicationRecordManagerImpl communicationManager;
    @Autowired
    private CustomerCompanyManagerImpl companyManager;
    @Autowired
    private CustomerInfoManagerImpl customerInfoManager;
    @Autowired
    private FinancialProfileMapper financialProfileMapper;
    @Autowired
    private CsProfileMapper csProfileMapper;

    private static final Map<String, String> VAT_TYPE_NAMES = Map.of(
            "SMALL_SCALE", "小规模纳税人",
            "COMMONLY", "一般纳税人"
    );
    private static final Map<Integer, String> INDIVIDUAL_TAX_RESULT_NAMES = Map.of(
            0, "未执行", 1, "验证失败", 2, "已验证"
    );

    /** 客户档案分页列表 */
    public IPage<ProfilePageResp> getProfilePage(GetProfilePageReq req, Long tenantId) {
        // 一条 SQL 联表查询：customer_company + customer_info + cs_customer_profile
        //   + financial.fxy_financial_account_sets + financial.individual_tax（子查询取最新）
        // EXISTS 过滤：该公司在合同表中有"代账报税-正式"或"代账报税-体验"的合同
        IPage<ProfilePageResp> page = csProfileMapper.getProfilePage(
                new Page<>(req.getPageNum(), req.getPageSize()), req, tenantId);

        List<ProfilePageResp> records = page.getRecords();
        if (records.isEmpty()) return page;

        // 填充中文名称（vatTypeName / individualTaxResultName）
        // 批量查办税员（一对多，单独一次 IN 查询）
        List<Long> accountSetIds = records.stream()
                .filter(r -> r.getAccountSetId() != null)
                .map(ProfilePageResp::getAccountSetId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, List<FinancialTaxOfficer>> taxOfficerMap = accountSetIds.isEmpty() ? Map.of() :
                financialProfileMapper.getTaxOfficersByAccountSetIds(accountSetIds)
                        .stream().collect(Collectors.groupingBy(FinancialTaxOfficer::getAccountSetId));

        for (ProfilePageResp resp : records) {
            if (resp.getVatType() != null) {
                resp.setVatTypeName(VAT_TYPE_NAMES.getOrDefault(resp.getVatType(), resp.getVatType()));
            }
            if (resp.getAccountSetId() != null) {
                List<FinancialTaxOfficer> officers =
                        taxOfficerMap.getOrDefault(resp.getAccountSetId(), Collections.emptyList());
                resp.setTaxOfficers(officers.stream().map(o -> {
                    ProfilePageResp.TaxOfficerItem item = new ProfilePageResp.TaxOfficerItem();
                    item.setId(o.getId());
                    item.setName(o.getName());
                    item.setPhone(o.getPhone());
                    item.setPassword(o.getPassword());
                    item.setLoginType(o.getLoginType());
                    item.setIsDefault(o.getIsDefault());
                    item.setRemark(o.getRemark());
                    return item;
                }).collect(Collectors.toList()));
            }
        }

        return page;
    }

    /** 保存/更新客户档案（仅 finmaster 侧字段） */
    public Boolean saveProfile(SaveProfileReq req, Long tenantId, LoginUser loginUser) {
        CsCustomerProfile profile = profileManager.lambdaQuery()
                .eq(CsCustomerProfile::getCompanyId, req.getCompanyId())
                .eq(CsCustomerProfile::getTenantId, tenantId)
                .one();
        if (profile == null) {
            profile = new CsCustomerProfile();
            profile.setCompanyId(req.getCompanyId());
            profile.setTenantId(tenantId);
            profile.setCreateBy(loginUser.getUsername());
            profile.setCreateByName(loginUser.getRealName());
            profile.setCreateTime(new Date());
        }
        profile.setBookkeepingStatus(req.getBookkeepingStatus());
        profile.setFilingStatus(req.getFilingStatus());
        profile.setStarLevel(req.getStarLevel());
        profile.setRemark(req.getRemark());
        profile.setUpdateBy(loginUser.getUsername());
        profile.setUpdateByName(loginUser.getRealName());
        profile.setUpdateTime(new Date());
        return profileManager.saveOrUpdate(profile);
    }

    /** 获取沟通记录列表 */
    public List<CsCommunicationRecord> getCommunicationList(Long companyId) {
        return communicationManager.lambdaQuery()
                .eq(CsCommunicationRecord::getCompanyId, companyId)
                .orderByDesc(CsCommunicationRecord::getCommunicateTime)
                .list();
    }

    /** 添加沟通记录 */
    public Boolean addCommunication(AddCommunicationReq req, Long tenantId, LoginUser loginUser) {
        CsCommunicationRecord record = new CsCommunicationRecord();
        record.setTenantId(tenantId);
        record.setCompanyId(req.getCompanyId());
        record.setCommunicateType(req.getCommunicateType());
        record.setCommunicateTime(req.getCommunicateTime() != null ? req.getCommunicateTime() : LocalDateTime.now());
        record.setContent(req.getContent());
        record.setRemark(req.getRemark());
        record.setOperatorId(loginUser.getUserId());
        record.setOperatorName(loginUser.getRealName());
        record.setCreateBy(loginUser.getUsername());
        record.setCreateByName(loginUser.getRealName());
        record.setCreateTime(new Date());
        record.setUpdateTime(new Date());
        return communicationManager.save(record);
    }

    /** 删除沟通记录 */
    public Boolean deleteCommunication(Long id) {
        return communicationManager.removeById(id);
    }
}
