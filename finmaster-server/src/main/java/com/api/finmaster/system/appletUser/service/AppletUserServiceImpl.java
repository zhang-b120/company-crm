package com.qykj.finmaster.system.appletUser.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.WebUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.CustomerInfoManager;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.system.appletUser.domain.AppletUser;
import com.qykj.finmaster.system.appletUser.domain.AppletUserRecommend;
import com.qykj.finmaster.system.appletUser.domain.req.AddAppletUserRecommendReq;
import com.qykj.finmaster.system.appletUser.domain.req.AddAppletUserReq;
import com.qykj.finmaster.system.appletUser.domain.req.GetAppletUserInfoReq;
import com.qykj.finmaster.system.appletUser.domain.resp.GetAppletUserInfoResp;
import com.qykj.finmaster.system.appletUser.domain.resp.GetEffectiveUserInfoResp;
import com.qykj.finmaster.system.appletUser.manager.impl.AppletUserManagerImpl;
import com.qykj.finmaster.system.appletUser.manager.impl.AppletUserRecommendManagerImpl;
import com.qykj.finmaster.system.appletUser.mapper.AppletUserMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序用户服务实现类
 */
@Service
public class AppletUserServiceImpl{

    @Autowired
    private AppletUserManagerImpl appletUserManager;
    @Autowired
    private CustomerInfoManager customerInfoManager;
    @Autowired
    private CustomerCompanyManager customerCompanyManager;
    @Autowired
    private AppletUserRecommendManagerImpl appletUserRecommendManager;

    /**
     * 小程序分享注册用户账户信息
     * @param req
     * @return
     */
    public Boolean addAppletUserByShare(AddAppletUserReq req) {
        //首先校验客户和公司信息
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getOpenId, req.getShareOpenId())
                .one();
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getOpenid, req.getShareOpenId())
                .eq(CustomerCompany::getId, req.getCompanyId())
                .one();
        if(ObjectUtils.isNotEmpty(customerInfo) && ObjectUtils.isNotEmpty(customerCompany)){
            //校验是否已存在用户
            List<AppletUser> list = appletUserManager.lambdaQuery()
                    .eq(AppletUser::getOpenId, req.getOpenId())
                    .list();
            if(CollUtil.isEmpty(list)) {
                AppletUser appletUser = CopierCopyUtils.convertTo(req, AppletUser.class);
                appletUser.setSource(GlobalConstant.Number.ONE_INT);
                appletUserManager.save(appletUser);
            }
            //校验是否已有权限信息
            CustomerCompany checkCustomerCompany = customerCompanyManager.lambdaQuery()
                    .eq(CustomerCompany::getTaxNumber, customerCompany.getTaxNumber())
                    .eq(CustomerCompany::getOpenid, req.getOpenId())
                    .one();
            if(ObjectUtils.isEmpty(checkCustomerCompany)){
                customerCompany.setOpenid(req.getOpenId());
                customerCompany.setId(null);
                customerCompany.setCreateTime(new Date());
                customerCompany.setUpdateTime(new Date());
                if(CollUtil.isEmpty(customerCompanyManager.lambdaQuery().eq(CustomerCompany::getOpenid, req.getOpenId()).list())){
                    customerCompany.setEnabled(GlobalConstant.Number.ONE_INT);
                }else{
                    customerCompany.setEnabled(GlobalConstant.Number.ZERO_INT);
                }
                customerCompanyManager.save(customerCompany);
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 获取登录用户基本信息
     * @param req
     * @return
     */
    public GetAppletUserInfoResp getAppletUserInfo(GetAppletUserInfoReq req) {
        GetAppletUserInfoResp resp = new GetAppletUserInfoResp();
        //获取有效用户
        AppletUser appletUser = appletUserManager.lambdaQuery()
                .eq(AppletUser::getOpenId, req.getOpenId())
                .eq(AppletUser::getEnabled, GlobalConstant.Number.ONE_INT)
                .eq(AppletUser::getDeleted, GlobalConstant.Number.ZERO_INT)
                .one();
        if(ObjectUtils.isEmpty(appletUser)){
            return resp;
        }
        resp.setIsAppletUser(Boolean.TRUE);
        //因为授权用户没有客户信息，直接在公司信息中获取
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getOpenid, req.getOpenId())
                .eq(CustomerCompany::getEnabled, GlobalConstant.Number.ONE_INT)
                .one();
        if(ObjectUtils.isNotEmpty(customerCompany)){
            resp.setName(customerCompany.getCompanyName());
            resp.setCompanyId(customerCompany.getId());
            resp.setCreditCode(customerCompany.getTaxNumber());
            resp.setIsVerifyTax(customerCompany.getIsVerifyTax());
            resp.setIsInvoicing(customerCompany.getIsInvoicing());
            if(GlobalConstant.Number.ONE_INT == customerCompany.getIsVerifyTax()){
                if(StringUtils.isNotEmpty(resp.getCreditCode())){
                    JSONObject dataJson = FinancialUtils.getAccountSets(resp.getCreditCode());
                    if(ObjectUtils.isNotEmpty(dataJson)){
                        resp.setAccountSetsId(dataJson.getLong("id").toString());
                        resp.setCurrentAccountDate(dataJson.getString("currentAccountDate"));
                        resp.setEnableDate(dataJson.getString("enableDate"));
                    }
                }
            }else{
                // 优先用自己 CC 的体验账套；为空时回退到家族（同 tax_number）任一 CC 的 experienceAccountSetId
                Long experienceAccountSetId = customerCompany.getExperienceAccountSetId();
                if (ObjectUtils.isEmpty(experienceAccountSetId) && StringUtils.isNotEmpty(customerCompany.getTaxNumber())) {
                    CustomerCompany familyCC = customerCompanyManager.lambdaQuery()
                            .eq(CustomerCompany::getTaxNumber, customerCompany.getTaxNumber())
                            .isNotNull(CustomerCompany::getExperienceAccountSetId)
                            .orderByDesc(CustomerCompany::getUpdateTime)
                            .last("LIMIT 1")
                            .one();
                    if (familyCC != null) {
                        experienceAccountSetId = familyCC.getExperienceAccountSetId();
                    }
                }
                if(ObjectUtils.isNotEmpty(experienceAccountSetId)){
                    JSONObject dataJson = FinancialUtils.getExperienceAccountSets(experienceAccountSetId);
                    if(ObjectUtils.isNotEmpty(dataJson)){
                        resp.setAccountSetsId(dataJson.getLong("id").toString());
                        resp.setCurrentAccountDate(dataJson.getString("currentAccountDate"));
                        resp.setEnableDate(dataJson.getString("enableDate"));
                    }
                }
            }
        }
        return resp;
    }

    /**
     * 拼装数据
     * @param username
     * @param nickName
     * @param openId
     * @param source
     * @return
     */
    public AppletUser getAppletUser(String username, String nickName, String openId, Integer source){
        AppletUser appletUser = new AppletUser();
        appletUser.setUsername(username);
        appletUser.setPhone(username);
        appletUser.setNickName(nickName);
        appletUser.setOpenId(openId);
        appletUser.setSource(source);
        return appletUser;
    }

    /**
     * 小程序推荐用户
     * @param req
     * @return
     */
    public Boolean addAppletUserRecommend(AddAppletUserRecommendReq req) {
        try {
            List<AppletUserRecommend> list = appletUserRecommendManager.lambdaQuery()
                    .eq(AppletUserRecommend::getRecommendedOpenId, req.getRecommendedOpenId())
                    .list();
            if(CollUtil.isNotEmpty(list)){
                return Boolean.TRUE;
            }
            AppletUserRecommend appletUserRecommend = CopierCopyUtils.convertTo(req, AppletUserRecommend.class);
            appletUserRecommend.setCreateTime(new Date());
            appletUserRecommendManager.save(appletUserRecommend);
        }catch (Exception e){
            //异常不做处理，以免影响小程序正常登录
        }
        return Boolean.TRUE;
    }

    /**
     * 获取推荐有效客户信息
     * @param openId
     * @return
     */
    public GetEffectiveUserInfoResp getEffectiveUserInfo(String openId) {
        GetEffectiveUserInfoResp resp = new GetEffectiveUserInfoResp();
        List<AppletUserRecommend> effectiveUserList = appletUserRecommendManager.getBaseMapper().getEffectiveUser(openId);
        if(CollUtil.isEmpty(effectiveUserList)){
            return resp;
        }
        resp.setEffectiveUserCount(effectiveUserList.size());
        resp.setRewardAmount(new BigDecimal(100).multiply(new BigDecimal(effectiveUserList.size())));
        return resp;
    }
}