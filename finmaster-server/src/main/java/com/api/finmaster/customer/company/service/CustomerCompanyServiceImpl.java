package com.qykj.finmaster.customer.company.service;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.util.StringUtil;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.FinancialConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.LogMessage;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.ProvinceParser;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.common.utils.third.QccUtil;
import com.qykj.finmaster.common.utils.third.req.CreateAccountSetReq;
import com.qykj.finmaster.common.utils.third.req.QccSearchReq;
import com.qykj.finmaster.common.utils.third.resp.QccSearchResp;
import com.qykj.finmaster.customer.company.domain.req.*;
import com.qykj.finmaster.customer.company.domain.resp.GetCustomerCompanyInfoResp;
import com.qykj.finmaster.customer.company.domain.resp.GetCustomerCompanyListResp;
import com.qykj.finmaster.customer.company.domain.resp.GetCustomerCompanyPageResp;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyLogManagerImpl;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.customer.leadLog.service.LeadLogServiceImpl;
import com.qykj.finmaster.customer.customerInfoLog.service.CustomerLogServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.appletUser.domain.AppletUser;
import com.qykj.finmaster.system.appletUser.manager.AppletUserManager;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.manager.impl.UserManagerImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.qykj.finmaster.customer.company.mapper.CustomerCompanyMapper;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.domain.CustomerCompanyLog;
import org.springframework.transaction.annotation.Transactional;

/**
 * 客户公司信息Service业务层处理
 */
@lombok.extern.slf4j.Slf4j
@Service
public class CustomerCompanyServiceImpl {

    @Autowired
    private CustomerCompanyMapper customerCompanyMapper;

    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;

    @Autowired
    private CustomerInfoManagerImpl customerInfoManager;

    @Autowired
    private LeadManagerImpl leadManager;

    @Autowired
    private CustomerLogServiceImpl customerLogService;

    @Autowired
    private LeadLogServiceImpl leadLogService;
    @Autowired
    private AppletUserManager appletUserManager;
    @Autowired
    private CustomerCompanyLogServiceImpl customerCompanyLogService;
    @Autowired
    private UserManagerImpl userManager;
    @Autowired
    private SysDictDataServiceImpl sysDictDataService;
    @Autowired
    private CustomerCompanyLogManagerImpl customerCompanyLogManager;
    @Autowired
    private com.qykj.finmaster.customer.company.manager.impl.CompanyLegalPersonManagerImpl companyLegalPersonManager;
    @Autowired
    private CustomerCompanyBroadcaster customerCompanyBroadcaster;
    @Autowired
    private com.qykj.finmaster.customer.lead.mapper.LeadPhoneMapper leadPhoneMapper;
    @Autowired
    private com.qykj.finmaster.customer.lead.manager.LeadPhoneManager leadPhoneManager;
    @Autowired
    private com.qykj.finmaster.customer.lead.service.LeadNotificationService leadNotificationService;

    /**
     * 按税号查询公司信息（仅查本地数据库）
     *   命中：返回 CompanyLookupResp（含 公司基本信息 + 工商信息 + 股东信息）
     *   未命中：返回 null
     * 同税号本地存在多条时，按 update_time DESC 取最新一条
     */
    public com.qykj.finmaster.customer.company.domain.resp.CompanyLookupResp lookupByTaxNumber(String taxNumber, Long tenantId) {
        if (StringUtil.isEmpty(taxNumber)) return null;
        com.qykj.finmaster.customer.company.domain.CustomerCompany local = customerCompanyManager.lambdaQuery()
                .eq(com.qykj.finmaster.customer.company.domain.CustomerCompany::getTaxNumber, taxNumber)
                .eq(tenantId != null, com.qykj.finmaster.customer.company.domain.CustomerCompany::getTenantId, tenantId)
                .orderByDesc(com.qykj.finmaster.customer.company.domain.CustomerCompany::getUpdateTime)
                .last("LIMIT 1")
                .one();
        if (local == null) return null;
        com.qykj.finmaster.customer.company.domain.resp.CompanyLookupResp resp =
                new com.qykj.finmaster.customer.company.domain.resp.CompanyLookupResp();
        resp.setTaxNumber(taxNumber);
        // 公司基本信息
        resp.setCompanyName(local.getCompanyName());
        resp.setCompanyType(local.getCompanyType());
        // 工商信息
        resp.setLegalName(local.getLegalName());
        resp.setLegalIdCard(local.getLegalIdCard());
        resp.setLegalPhone(local.getLegalPhone());
        resp.setRegisteredAddress(local.getRegisteredAddress());
        resp.setEstablishedDate(local.getEstablishedDate() != null ? local.getEstablishedDate().toString() : null);
        resp.setRegStatus(local.getRegStatus());
        resp.setProvince(local.getProvince());
        resp.setTaxDistrict(local.getTaxDistrict());
        resp.setIsInvoicing(local.getIsInvoicing());
        resp.setIsIndividual(local.getIsIndividual());
        resp.setCompanyStatus(local.getCompanyStatus());
        // 股东信息（company_legal_person 表）
        try {
            java.util.List<com.qykj.finmaster.customer.company.domain.CompanyLegalPerson> persons =
                    companyLegalPersonManager.lambdaQuery()
                            .eq(com.qykj.finmaster.customer.company.domain.CompanyLegalPerson::getCompanyId, local.getId())
                            .list();
            java.util.List<com.qykj.finmaster.customer.company.domain.resp.CompanyLookupResp.Shareholder> list =
                    new java.util.ArrayList<>();
            for (com.qykj.finmaster.customer.company.domain.CompanyLegalPerson p : persons) {
                com.qykj.finmaster.customer.company.domain.resp.CompanyLookupResp.Shareholder s =
                        new com.qykj.finmaster.customer.company.domain.resp.CompanyLookupResp.Shareholder();
                s.setId(p.getId());
                s.setName(p.getName());
                s.setIdCard(p.getIdCard());
                s.setPhone(p.getPhone());
                s.setShareholdingRatio(p.getShareholdingRatio());
                s.setRemark(p.getRemark());
                list.add(s);
            }
            resp.setShareholders(list);
        } catch (Exception ignore) {
            resp.setShareholders(java.util.Collections.emptyList());
        }
        return resp;
    }

    /**
     * 查询客户公司信息
     */
    public GetCustomerCompanyInfoResp selectCustomerCompanyById(Long id, Long tenantId) {
        GetCustomerCompanyInfoResp resp = new GetCustomerCompanyInfoResp();
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, id)
                .eq(ObjectUtils.isNotEmpty(id), CustomerCompany::getTenantId, tenantId)
                .one();
        if(ObjectUtils.isNotEmpty(customerCompany)){
            resp = CopierCopyUtils.convertTo(customerCompany, GetCustomerCompanyInfoResp.class);
        }
        return resp;
    }

    /**
     * 查询客户公司信息列表
     */
    public List<GetCustomerCompanyPageResp> selectCustomerCompanyList(GetCustomerCompanyPageReq req,Long tenantId) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        params.put("tenantId", tenantId);
        List<GetCustomerCompanyPageResp> result = customerCompanyMapper.getCustomerCompanyList(params);
        return result;
    }

    /**
     * 根据客户ID查询公司信息列表
     */
    public List<GetCustomerCompanyInfoResp> selectCustomerCompanyListByCustomerId(Long customerId,Long tenantId) {
        List<CustomerCompany> customerCompanyList = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getCustomerId, customerId)
                .eq(ObjectUtils.isNotEmpty(tenantId), CustomerCompany::getTenantId, tenantId)
                .list();

        if(CollUtil.isEmpty(customerCompanyList)){
            return Collections.emptyList();
        }
        return CopierCopyUtils.convertTo(customerCompanyList, GetCustomerCompanyInfoResp.class);
    }

    /**
     * 新增客户公司信息
     */
    public int insertCustomerCompany(CustomerCompany customerCompany) {
        return customerCompanyMapper.insertCustomerCompany(customerCompany);
    }

    /**
     * 修改客户公司信息
     */
    @Transactional
    public Boolean updateCustomerCompany(UpdateCustomerCompanyReq req, Long tenantId, LoginUser loginUser) {
        CustomerCompany before = customerCompanyManager.getById(req.getId());
        if(ObjectUtils.isEmpty(before)){
            throw new BusinessException(ResultMsg.CUSTOMER_COMPANY_NOT_DATA);
        }
        CustomerCompany customerCompany = CopierCopyUtils.convertTo(req, CustomerCompany.class);
        // 注册地址变更或前端没传 province/taxDistrict 时，按地址自动识别
        if (StringUtils.isBlank(customerCompany.getRegisteredAddress())) {
            customerCompany.setRegisteredAddress(before.getRegisteredAddress());
        }
        sanitizeDirtyStrings(customerCompany);
        ProvinceParser.fillIfBlank(customerCompany);
        customerCompany.setUpdateBy(loginUser.getUsername());
        customerCompany.setUpdateByName(loginUser.getRealName());
        customerCompany.setUpdateTime(new Date());
        customerCompanyManager.getBaseMapper().updateById(customerCompany);
        // 修复：req 不带 customerId，convertTo 后 customerCompany.customerId 会是 null。
        // customer_company_log.customer_id 是 NOT NULL 列，必须用 DB 已有的 before.customerId 来记录归属。
        Long ownerCustomerId = before.getCustomerId();
        if (ownerCustomerId != null) {
            customerCompanyLogService.addLogByUpdate(ownerCustomerId, GlobalConstant.Number.TWO_INT,
                    MessageFormat.format(LogMessage.UPDATE_COMPANY_LOG,
                            before.getCompanyName() + "->" + req.getCompanyName(),
                            before.getLegalName() + "->" + req.getLegalName(),
                            before.getCompanyType() + "->" + req.getCompanyType()),
                    loginUser.getUserId(), loginUser.getUsername(), tenantId);
        }
        return Boolean.TRUE;
    }


    /**
     * 删除客户公司信息信息
     */
    public int deleteCustomerCompanyById(Long id) {
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, id)
                .one();
        if (customerCompany == null) {
            throw new BusinessException("公司不存在或已被删除");
        }
        if (customerCompany.getIsVerifyTax() != null && customerCompany.getIsVerifyTax() == GlobalConstant.Number.ONE_INT) {
            throw new BusinessException("当前公司已验证税密，无法删除");
        }
        if (customerCompany.getEnabled() != null && customerCompany.getEnabled() == GlobalConstant.Number.ONE_INT) {
            throw new BusinessException("当前公司处于启用状态，无法删除");
        }
        return customerCompanyManager.getBaseMapper().deleteById(id);
    }

    /**
     * 查询客户公司操作日志列表
     *
     * @param req 客户公司操作日志信息
     * @return 客户公司操作日志集合
     */
    public List<CustomerCompanyLog> selectCustomerCompanyLogList(GetCustomerCompanyLogReq req, Long tenantId) {
        List<CustomerCompanyLog> list = customerCompanyLogManager.lambdaQuery()
                .eq(ObjectUtils.isNotEmpty(req.getCustomerId()), CustomerCompanyLog::getCustomerId, req.getCustomerId())
                .eq(ObjectUtils.isNotEmpty(req.getLogType()), CustomerCompanyLog::getLogType, req.getLogType())
                .eq(ObjectUtils.isNotEmpty(tenantId), CustomerCompanyLog::getTenantId, tenantId)
                .orderByDesc(CustomerCompanyLog::getCreateTime)
                .list();
        if(ObjectUtils.isEmpty(list)){
            return Collections.emptyList();
        }
        return list;
    }

    /**
     * 新增客户公司操作日志
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertCustomerCompanyLog(AddCustomerCompanyLogReq req, Long tenantId, LoginUser loginUser) {
        CustomerCompanyLog customerCompanyLog = CopierCopyUtils.convertTo(req, CustomerCompanyLog.class);
        // 设置租户ID
        customerCompanyLog.setTenantId(tenantId);
        // 操作人
        if(ObjectUtils.isEmpty(customerCompanyLog.getOperatorId())){
            customerCompanyLog.setOperatorId(loginUser.getUserId());
            customerCompanyLog.setOperatorName(loginUser.getRealName());
        }
        return customerCompanyLogManager.save(customerCompanyLog);
    }

    /**
     * 小程序新增公司
     * @param req
     * @return
     * @Description
     *  1.客户添加公司的时候，如果不存在客户，创建客户
     *  2.创建客户时，如果线索推荐码，创建为私海客户，并添加负责人
     *  3.如果存在客户。通过openID可以查询到，在添加推荐码时必定会添加负责人，此时无需再次处理
     *      通过手机号查询到，则代表客户不是通过线索转化的，需要给客户赋值openID，如果线索存在推荐码，还需给客户添加负责人并转为私海客户
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean uaAddCustomerCompany(UaAddCustomerCompanyReq req) {
        AppletUser appletUser = appletUserManager.lambdaQuery()
                .eq(AppletUser::getOpenId, req.getOpenid())
                .one();
        if(GlobalConstant.Number.ONE_INT == appletUser.getSource()){
            return Boolean.FALSE;
        }
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getTaxNumber, req.getTaxNumber())
                .eq(CustomerCompany::getOpenid, req.getOpenid())
                .one();
        if(ObjectUtils.isNotEmpty(customerCompany)){
            return Boolean.TRUE;
        }
        customerCompany = CopierCopyUtils.convertTo(req, CustomerCompany.class);
        if(CollUtil.isEmpty(customerCompanyManager.lambdaQuery().eq(CustomerCompany::getOpenid, req.getOpenid()).list())){
            customerCompany.setEnabled(GlobalConstant.Number.ONE_INT);
        }else{
            customerCompany.setEnabled(GlobalConstant.Number.ZERO_INT);
        }
        customerCompany.setIsVerifyTax(GlobalConstant.Number.ZERO_INT);
        //首先根据openId判断是否存在客户
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getOpenId, req.getOpenid())
                .one();
        if (ObjectUtils.isEmpty(customerInfo)) {
            //根据手机号判断客户
            customerInfo = customerInfoManager.lambdaQuery()
                    .eq(CustomerInfo::getPhone, appletUser.getPhone())
                    .one();
            // 新增反查路径：按 openId / phone 在 lead_phone 子表里找已被合并的线索，
            // 取该线索下默认承接客户（lead_phone.default_customer_id）或任一已转化客户。
            if (ObjectUtils.isEmpty(customerInfo)) {
                customerInfo = resolveCustomerByLeadPhoneFamily(req.getOpenid(), appletUser.getPhone());
            }
            Lead lead = leadManager.lambdaQuery()
                    .eq(Lead::getOpenId, req.getOpenid())
                    .one();
            // 主表没命中时也尝试通过 lead_phone 子表反查 lead
            if (ObjectUtils.isEmpty(lead)) {
                List<Long> hitLeadIds = leadPhoneMapper.selectLeadIdsByContactInPhoneTable(
                        appletUser.getPhone(), req.getOpenid(), null);
                if (CollUtil.isNotEmpty(hitLeadIds)) {
                    lead = leadManager.getById(hitLeadIds.get(0));
                }
            }
            User user = null;
            if(ObjectUtils.isNotEmpty(lead) && ObjectUtils.isNotEmpty(lead.getRecommendCode())) {
                user = userManager.lambdaQuery()
                        .eq(User::getId, lead.getRecommendCode())
                        .one();
            }
            // 1.CustomerInfo为空，执行后续操作;不为空,给openId赋值;
            // 2.如果线索含有推荐码，给客户添加负责人
            if(ObjectUtils.isEmpty(customerInfo)) {
                //客户信息直接在线索中获取
                customerInfo = CopierCopyUtils.convertTo(lead, CustomerInfo.class);
                customerInfo.setId(null);
                customerInfo.setCustomerName(customerCompany.getCompanyName());
                customerInfo.setStatus(GlobalConstant.Number.ONE_INT);
                if(StringUtils.isNotEmpty(customerInfo.getOwnerUserName())
                        && StringUtils.isNotEmpty(customerInfo.getOwnerUser())){
                    customerInfo.setCustomerType(GlobalConstant.Number.ONE_INT);
                }else {
                    customerInfo.setCustomerType(GlobalConstant.Number.TWO_INT);
                }
                customerInfo.setLeadId(lead.getId());
                customerInfo.setCreateTime(new Date());
                customerInfo.setUpdateTime(new Date());
                //存在推荐人给客户添加负责人
                if(ObjectUtils.isNotEmpty(user)){
                    customerInfo.setCustomerType(GlobalConstant.Number.ONE_INT);
                    customerInfo.setCreateBy(user.getUsername());
                    customerInfo.setCreateByName(user.getRealName());
                    customerInfo.setOwnerUser(user.getUsername());
                    customerInfo.setOwnerUserName(user.getRealName());
                }
                customerInfoManager.save(customerInfo);
                customerLogService.recordCustomerLog(customerInfo.getId(), customerInfo.getCustomerType(),
                        GlobalConstant.Number.ONE_INT, MessageFormat.format(LogMessage.CREATE_CUSTOMER,
                                customerInfo.getCustomerName()), null, null, customerInfo.getTenantId());
                //修改线索跟进状态
                leadManager.lambdaUpdate()
                        .set(Lead::getStatus, GlobalConstant.Number.THREE_INT)
                        .eq(Lead::getId, lead.getId())
                        .update();
                leadLogService.addLeadLogByCommon(lead.getId(), GlobalConstant.Number.FOUR_INT,
                        customerInfo.getCustomerType() == GlobalConstant.Number.ONE_INT
                                ? MessageFormat.format(LogMessage.UPDATE_LEAD_LOG_PRIVATE, customerInfo.getCustomerName(), customerInfo.getOwnerUserName())
                                : MessageFormat.format(LogMessage.UPDATE_LEAD_LOG_PUBLIC, customerInfo.getCustomerName()));
            }else{
                LambdaUpdateWrapper<CustomerInfo> wrapper = Wrappers.lambdaUpdate(CustomerInfo.class);
                wrapper.set(CustomerInfo::getOpenId, req.getOpenid());
                if(ObjectUtils.isNotEmpty(user) && StringUtils.isEmpty(customerInfo.getOwnerUser()) && StringUtils.isEmpty(customerInfo.getOwnerUserName())){
                    wrapper.set(CustomerInfo::getOwnerUser, user.getUsername())
                            .set(CustomerInfo::getOwnerUserName, user.getRealName());
                }
                wrapper.eq(CustomerInfo::getPhone, customerInfo.getPhone());
                customerInfoManager.update(wrapper);
            }
        }
        //公司信息添加客户ID
        customerCompany.setCustomerId(customerInfo.getId());
        customerCompany.setTenantId(customerInfo.getTenantId());
        sanitizeDirtyStrings(customerCompany);
        customerCompanyManager.save(customerCompany);
        customerCompanyLogService.addLog(customerInfo.getId(), customerInfo.getCustomerName(), GlobalConstant.Number.ONE_INT,
                MessageFormat.format(LogMessage.CREATE_COMPANY_LOG, customerCompany.getCompanyName()), customerCompany.getTenantId());

        // 给关联线索的负责人推送"用户 XXX 已成功创建公司"
        try {
            if (customerInfo.getLeadId() != null) {
                com.qykj.finmaster.customer.lead.entity.Lead lead = leadManager.getById(customerInfo.getLeadId());
                if (lead != null && org.apache.commons.lang3.StringUtils.isNotEmpty(lead.getOwnerUser())) {
                    String userIdentifier = appletUser != null && org.apache.commons.lang3.StringUtils.isNotEmpty(appletUser.getPhone())
                            ? appletUser.getPhone()
                            : customerInfo.getPhone();
                    leadNotificationService.notifyAppletCreateCompany(lead, userIdentifier, customerCompany.getCompanyName());
                }
            }
        } catch (Exception ex) {
            log.warn("[uaAddCustomerCompany] 推送创建公司通知失败 customerId={} err={}",
                    customerInfo.getId(), ex.getMessage());
        }
        return Boolean.TRUE;
    }

    /**
     * 获取当前用户所有公司
     * @param req
     * @return
     */
    public List<GetCustomerCompanyListResp> getCustomerCompanyList(GetCustomerCompanyListReq req) {
        //因为授权用户没有客户信息，直接在公司信息中获取
        List<CustomerCompany> customerCompanyList = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getOpenid, req.getOpenId())
                .list();

        if(CollUtil.isEmpty(customerCompanyList)){
            return Collections.emptyList();
        }

        List<String> taxNumberList = customerCompanyList.stream()
                .filter(f -> StringUtil.isNotEmpty(f.getTaxNumber()) && GlobalConstant.Number.ONE_INT == f.getIsVerifyTax())
                .map(CustomerCompany::getTaxNumber)
                .distinct()
                .collect(Collectors.toList());
        Map<String, JSONObject> collect = new HashMap<>();
        if(CollUtil.isNotEmpty(taxNumberList)){
            JSONArray dataJson = FinancialUtils.getGetAccountSetsList(taxNumberList);
            if(CollUtil.isNotEmpty(dataJson)){
                collect = dataJson.stream()
                        .filter(f -> ObjectUtils.isNotEmpty(f) && StringUtils.isNotEmpty(((JSONObject) f).getString("creditCode")))
                        .collect(Collectors.toMap(
                                item -> {
                                    return ((JSONObject) item).getString("creditCode");
                                },
                                item -> {
                                    return (JSONObject) item;
                                },
                                (k, v) -> v));
            }
        }
        Map<String, JSONObject> finalCollect = collect;
        Map<String, String> dictDataMap = sysDictDataService.getDictDataMapByTypeCode(DictConstants.COMPANY_TYPE);
        return customerCompanyList.stream().map(item ->{
            GetCustomerCompanyListResp resp = CopierCopyUtils.convertTo(item, GetCustomerCompanyListResp.class);
            if(MapUtil.isNotEmpty(finalCollect) && ObjectUtils.isNotEmpty(finalCollect.get(item.getTaxNumber()))){
                JSONObject jsonObject = finalCollect.get(item.getTaxNumber());
                resp.setAccountSetsId(jsonObject.getLong("id"));
                resp.setCurrentAccountDate(jsonObject.getString("currentAccountDate"));
                resp.setEnableDate(jsonObject.getString("enableDate"));
            }
            if(MapUtil.isNotEmpty(dictDataMap) && ObjectUtils.isNotEmpty(dictDataMap.get(item.getCompanyType()))){
                resp.setCompanyTypeLabel(dictDataMap.get(item.getCompanyType()));
            }
            return resp;
        }).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean changeCustomerCompany(Long companyId, String openId) {
        //修改原公司启用状态
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getEnabled, GlobalConstant.Number.ZERO_INT)
                .eq(CustomerCompany::getOpenid, openId)
                .eq(CustomerCompany::getEnabled, GlobalConstant.Number.ONE_INT)
                .update();
        //修改先公司为启用
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getEnabled, GlobalConstant.Number.ONE_INT)
                .eq(CustomerCompany::getId, companyId)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 查询客户公司信息列表（跨租户：需求要求全部租户的数据，因此不强制 tenant_id 过滤）
     * @param req
     * @return
     */
    public List<GetCustomerCompanyPageResp> getCustomerCompanyList(GetCustomerCompanyPageReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        List<GetCustomerCompanyPageResp> result = customerCompanyMapper.getCustomerCompanyList(params);
        if (CollUtil.isNotEmpty(result)) {
            // mapper 里已删 sys_dict_data 的低效 JOIN，公司类型名称改为这里查字典再回填
            Map<String, String> dictMap = sysDictDataService.getDictDataMapByTypeCode(DictConstants.COMPANY_TYPE);
            if (MapUtil.isNotEmpty(dictMap)) {
                for (GetCustomerCompanyPageResp r : result) {
                    if (StringUtils.isNotBlank(r.getCompanyType())) {
                        r.setCompanyTypeName(dictMap.get(r.getCompanyType()));
                    }
                }
            }
        }
        return result;
    }

    /**
     * 验证税密（验税）。
     *  - 主表 update 当前 ids 为 1
     *  - 走 broadcaster 把同 lead + 同税号家族里其它 CC 也广播为 1（升级方向无守门）
     */
    public Boolean verifyTax(List<Long> ids) {
        if(CollUtil.isEmpty(ids)){
            return Boolean.FALSE;
        }
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsVerifyTax, GlobalConstant.Number.ONE_INT)
                .in(CustomerCompany::getId, ids)
                .update();
        // 广播
        String operator = currentOperator();
        for (Long id : ids) {
            customerCompanyBroadcaster.broadcastSingleField(id, "isVerifyTax", GlobalConstant.Number.ONE_INT, operator);
        }
        return Boolean.TRUE;
    }

    /**
     * 取消验证税密（降级方向）。
     *  - 主表 update 当前 ids 为 0
     *  - 广播：仅当家族其它 CC 上无 PAID NORMAL 订单时才同步置 0（守门保护）
     */
    public Boolean cancelVerifyTax(List<Long> ids) {
        if(CollUtil.isEmpty(ids)){
            return Boolean.FALSE;
        }
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsVerifyTax, GlobalConstant.Number.ZERO_INT)
                .in(CustomerCompany::getId, ids)
                .update();
        String operator = currentOperator();
        for (Long id : ids) {
            customerCompanyBroadcaster.broadcastSingleField(id, "isVerifyTax", GlobalConstant.Number.ZERO_INT, operator);
        }
        return Boolean.TRUE;
    }

    /**
     * 通过 lead_phone 子表反查家族客户：
     *   1) phone 或 openId 命中 lead_phone → 找到一组 leadId
     *   2) 优先取子表行的 default_customer_id 对应客户
     *   3) 否则取这些 lead 下任一已有 customer_info
     *
     * 主要用于分支 2/3：合并后无客户的源 lead 之 openId 在小程序首次建公司时
     * 能挂到家族指定的客户上，避免再次产生孤儿客户。
     */
    private CustomerInfo resolveCustomerByLeadPhoneFamily(String openId, String phone) {
        if (StringUtils.isEmpty(openId) && StringUtils.isEmpty(phone)) return null;
        java.util.List<com.qykj.finmaster.customer.lead.entity.LeadPhone> rows = leadPhoneManager.lambdaQuery()
                .and(w -> {
                    boolean hit = false;
                    if (StringUtils.isNotEmpty(openId)) { w.eq(com.qykj.finmaster.customer.lead.entity.LeadPhone::getOpenId, openId); hit = true; }
                    if (StringUtils.isNotEmpty(phone))  { if (hit) w.or(); w.eq(com.qykj.finmaster.customer.lead.entity.LeadPhone::getPhone, phone); }
                })
                .list();
        if (CollUtil.isEmpty(rows)) return null;
        // 1) default_customer_id 优先
        for (com.qykj.finmaster.customer.lead.entity.LeadPhone r : rows) {
            if (r.getDefaultCustomerId() != null) {
                CustomerInfo c = customerInfoManager.getById(r.getDefaultCustomerId());
                if (c != null) return c;
            }
        }
        // 2) 这些 lead 下任一 customer
        java.util.List<Long> leadIds = rows.stream()
                .map(com.qykj.finmaster.customer.lead.entity.LeadPhone::getLeadId)
                .filter(java.util.Objects::nonNull)
                .distinct().toList();
        if (CollUtil.isEmpty(leadIds)) return null;
        return customerInfoManager.lambdaQuery()
                .in(CustomerInfo::getLeadId, leadIds)
                .orderByAsc(CustomerInfo::getCreateTime)
                .last("LIMIT 1")
                .one();
    }

    /** 取当前登录用户名（用于 broadcaster 审计） */
    private String currentOperator() {
        try {
            org.springframework.security.core.Authentication auth =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.qykj.finmaster.security.LoginUser u) {
                return u.getUsername();
            }
        } catch (Exception ignore) {}
        return "system";
    }

    /**
     * 修改公司状态
     */
    public Boolean updateCompanyStatus(Long id, String companyStatus, String statusRemark) {
        boolean ok = customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getCompanyStatus, companyStatus)
                .set(CustomerCompany::getStatusRemark, statusRemark)
                .set(CustomerCompany::getUpdateTime, new Date())
                .eq(CustomerCompany::getId, id)
                .update();
        // 家族广播：DEREGISTERED / COOPERATING 升级直接广；CANCELLED 走守门
        if (ok) {
            String operator = currentOperator();
            java.util.Map<String, Object> dirty = new java.util.LinkedHashMap<>();
            dirty.put("companyStatus", companyStatus);
            if (statusRemark != null) dirty.put("statusRemark", statusRemark);
            customerCompanyBroadcaster.broadcast(id, dirty, operator);
        }
        return ok;
    }

    /**
     * 修改客户公司信息（PC 编辑）。
     * 改完后把 companyName / legalName / companyType 三个客观属性广播给家族其它 CC。
     */
    public Boolean updateCompanyInfo(UpdateCustomerCompanyReq req, Long tenantId, LoginUser loginUser) {
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, req.getId())
                .one();

        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getCompanyName, blankIfDirty(req.getCompanyName()))
                .set(CustomerCompany::getLegalName, blankIfDirty(req.getLegalName()))
                .set(CustomerCompany::getCompanyType, req.getCompanyType())
                .set(CustomerCompany::getUpdateBy, loginUser.getUserId())
                .set(CustomerCompany::getUpdateByName, loginUser.getUsername())
                .set(CustomerCompany::getUpdateTime, new Date())
                .eq(CustomerCompany::getId, req.getId())
                .update();
        customerCompanyLogService.addLogByUpdate(customerCompany.getCustomerId(), GlobalConstant.Number.TWO_INT,
                MessageFormat.format(LogMessage.UPDATE_COMPANY_LOG,
                        customerCompany.getCompanyName() + "->"+ req.getCompanyName(),
                        customerCompany.getLegalName() + "->" +req.getLegalName(),
                        customerCompany.getCompanyType() + "->" +req.getCompanyType()),
                loginUser.getUserId(), loginUser.getUsername(), tenantId);
        // 家族广播：客观属性
        java.util.Map<String, Object> dirty = new java.util.LinkedHashMap<>();
        if (!java.util.Objects.equals(customerCompany.getCompanyName(), req.getCompanyName())) {
            dirty.put("companyName", req.getCompanyName());
        }
        if (!java.util.Objects.equals(customerCompany.getLegalName(), req.getLegalName())) {
            dirty.put("legalName", req.getLegalName());
        }
        if (!java.util.Objects.equals(customerCompany.getCompanyType(), req.getCompanyType())) {
            dirty.put("companyType", req.getCompanyType());
        }
        if (!dirty.isEmpty()) {
            customerCompanyBroadcaster.broadcast(req.getId(), dirty, loginUser.getUsername());
        }
        return Boolean.TRUE;
    }

    /**
     * 从企查查刷新公司的工商信息
     * 刷新字段：registeredAddress / establishedDate / regStatus / legalName
     * 匹配规则：优先按 taxNumber 匹配 creditCode，次按 companyName 匹配 name
     *
     * @return 刷新后的 CustomerCompany（前端需要用最新数据回写详情弹窗）
     */
    @Transactional(rollbackFor = Exception.class)
    public CustomerCompany refreshQccInfo(Long companyId, Long tenantId, LoginUser loginUser) {
        CustomerCompany company = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        if (company == null) {
            throw new BusinessException("公司不存在");
        }
        if (StringUtils.isBlank(company.getTaxNumber()) && StringUtils.isBlank(company.getCompanyName())) {
            throw new BusinessException("公司缺少税号和名称，无法查询企查查");
        }

        // 调企查查，优先用税号查，精度高
        QccSearchReq req = new QccSearchReq();
        req.setSearchKey(StringUtils.isNotBlank(company.getTaxNumber())
                ? company.getTaxNumber() : company.getCompanyName());
        List<QccSearchResp> results = QccUtil.getCompanyInfo(req);
        if (CollUtil.isEmpty(results)) {
            throw new BusinessException("企查查未返回结果，请核对税号 / 公司名");
        }

        // 精确匹配一条
        QccSearchResp matched = null;
        if (StringUtils.isNotBlank(company.getTaxNumber())) {
            matched = results.stream()
                    .filter(r -> company.getTaxNumber().equals(r.getCreditCode()))
                    .findFirst().orElse(null);
        }
        if (matched == null && StringUtils.isNotBlank(company.getCompanyName())) {
            matched = results.stream()
                    .filter(r -> company.getCompanyName().equals(r.getName()))
                    .findFirst().orElse(null);
        }
        if (matched == null) {
            throw new BusinessException("企查查返回 " + results.size()
                    + " 条，但均未精确匹配当前税号 / 公司名，无法自动刷新");
        }

        // 解析日期
        LocalDate establishedDate = null;
        if (StringUtils.isNotBlank(matched.getStartDate())) {
            try {
                establishedDate = LocalDate.parse(matched.getStartDate());
            } catch (Exception ignore) {
                // 企查查偶尔返回 "2020-01" 等非 ISO 格式，忽略
            }
        }

        LambdaUpdateWrapper<CustomerCompany> wrapper = Wrappers.lambdaUpdate(CustomerCompany.class)
                .eq(CustomerCompany::getId, companyId)
                .set(CustomerCompany::getUpdateTime, new Date());
        if (StringUtils.isNotBlank(matched.getAddress())) {
            wrapper.set(CustomerCompany::getRegisteredAddress, matched.getAddress());
        }
        if (establishedDate != null) {
            wrapper.set(CustomerCompany::getEstablishedDate, establishedDate);
        }
        if (StringUtils.isNotBlank(matched.getStatus())) {
            wrapper.set(CustomerCompany::getRegStatus, matched.getStatus());
        }
        if (StringUtils.isNotBlank(matched.getOperName())) {
            wrapper.set(CustomerCompany::getLegalName, matched.getOperName());
        }
        if (loginUser != null) {
            wrapper.set(CustomerCompany::getUpdateBy, String.valueOf(loginUser.getUserId()));
            wrapper.set(CustomerCompany::getUpdateByName, loginUser.getRealName());
        }
        customerCompanyManager.update(wrapper);

        // 写公司操作日志
        try {
            String summary = String.format(
                    "工商信息从企查查刷新：地址=%s 成立=%s 状态=%s 法人=%s",
                    nullToDash(matched.getAddress()),
                    establishedDate != null ? establishedDate.toString() : "-",
                    nullToDash(matched.getStatus()),
                    nullToDash(matched.getOperName())
            );
            customerCompanyLogService.addLogByUpdate(
                    company.getCustomerId(), GlobalConstant.Number.TWO_INT, summary,
                    loginUser != null ? loginUser.getUserId() : null,
                    loginUser != null ? loginUser.getUsername() : "system",
                    tenantId
            );
        } catch (Exception ignore) {
            // 日志失败不影响主流程
        }

        // 家族广播：QCC 刷新的全部是客观工商属性
        try {
            java.util.Map<String, Object> dirty = new java.util.LinkedHashMap<>();
            if (StringUtils.isNotBlank(matched.getAddress()))   dirty.put("registeredAddress", matched.getAddress());
            if (establishedDate != null)                         dirty.put("establishedDate", establishedDate);
            if (StringUtils.isNotBlank(matched.getStatus()))    dirty.put("regStatus", matched.getStatus());
            if (StringUtils.isNotBlank(matched.getOperName()))  dirty.put("legalName", matched.getOperName());
            if (!dirty.isEmpty()) {
                customerCompanyBroadcaster.broadcast(companyId, dirty,
                        loginUser != null ? loginUser.getUsername() : "system");
            }
        } catch (Exception ex) {
            log.warn("[refreshQccInfo] 广播失败 companyId={} err={}", companyId, ex.getMessage());
        }

        return customerCompanyManager.getById(companyId);
    }

    private static String nullToDash(String s) {
        return StringUtils.isBlank(s) ? "-" : s;
    }

    /**
     * 创建体验账套
     * @param companyId
     * @return
     */
    public String createExperienceAccountSet(Long companyId) {
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        //校验能否创建体验账套
        //未查到公司信息
        if(ObjectUtils.isEmpty(customerCompany)){
            return ResultMsg.CUSTOMER_COMPANY_CREATE_FAIL;
        }
        //存在正式账套
        JSONObject dataJson = FinancialUtils.getAccountSets(customerCompany.getTaxNumber());
        if(ObjectUtils.isNotEmpty(dataJson)){
            return ResultMsg.CUSTOMER_COMPANY_CREATE_FAIL_HAVE;
        }
        //已验证税密
        if(GlobalConstant.Number.ONE_INT == customerCompany.getIsVerifyTax()){
            return ResultMsg.CUSTOMER_COMPANY_CREATE_FAIL_FORMAL;
        }
        //当前公司已被其他人验证税密
        boolean flag = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getTaxNumber, customerCompany.getTaxNumber())
                .list().stream()
                .map(CustomerCompany::getIsVerifyTax)
                .anyMatch(item -> GlobalConstant.Number.ONE_INT == item);
        if(flag){
            return MessageFormat.format(ResultMsg.CUSTOMER_COMPANY_CREATE_FAIL_IS_HAVE, customerCompany.getCompanyName());
        }
        //已存在体验账套
        if(ObjectUtils.isNotEmpty(customerCompany.getExperienceAccountSetId())){
            return MessageFormat.format(ResultMsg.CUSTOMER_COMPANY_CREATE_FAIL_REPEAT, customerCompany.getCompanyName());
        }
        //没有公司类型
        if(StringUtils.isEmpty(customerCompany.getCompanyType())){
            return MessageFormat.format(ResultMsg.CUSTOMER_COMPANY_CREATE_FAIL_NOT_COMPANY_TYPE, customerCompany.getCompanyName());
        }
        //拼装请求参数
        CreateAccountSetReq req = getCreateAccountSetReq(customerCompany);
        //创建体验账套
        JSONObject result = FinancialUtils.createExperienceAccountSet(req);
        if(ObjectUtils.isNotEmpty(result)){
            Long accountSetId = Long.valueOf(result.get("id").toString());
            if(ObjectUtils.isNotEmpty(accountSetId)){
                customerCompanyManager.lambdaUpdate()
                        .set(CustomerCompany::getExperienceAccountSetId, accountSetId)
                        .eq(CustomerCompany::getId, customerCompany.getId())
                        .update();
                return MessageFormat.format(ResultMsg.CUSTOMER_COMPANY_CREATE_SUCCESS, customerCompany.getCompanyName());
            }
        }
        return ResultMsg.CUSTOMER_COMPANY_CREATE_FAIL;
    }

    /**
     * 拼装创建体验账套req
     * @param customerCompany
     * @return
     */
    private CreateAccountSetReq getCreateAccountSetReq(CustomerCompany customerCompany){
        CreateAccountSetReq req = new CreateAccountSetReq();
        req.setCompanyName(customerCompany.getCompanyName());
        req.setCreditCode(customerCompany.getTaxNumber());
        if(DictConstants.COMPANY_TYPE_SCALE.equals(customerCompany.getCompanyType())){
            req.setAstId(FinancialConstants.TEMPLATE_ID_SMALL_SCALE);
            req.setVatType(FinancialConstants.VAT_TYPE_SMALL_SCALE);
        }else if(DictConstants.COMPANY_TYPE_GENERAL.equals(customerCompany.getCompanyType())){
            req.setAstId(FinancialConstants.TEMPLATE_ID_COMMONLY);
            req.setVatType(FinancialConstants.VAT_TYPE_COMMONLY);
        }
        req.setEnableDate(LocalDate.now().with(TemporalAdjusters.firstDayOfMonth()));
        req.setElectricityTaxPhone(FinancialConstants.ELECTRICITY_TAX_PHONE);
        req.setElectricityTaxCode(FinancialConstants.ELECTRICITY_TAX_CODE);
        req.setZeroDeclarations(Boolean.FALSE);
        req.setVoucherReviewed(Boolean.FALSE);
        return req;
    }

    /**
     * 开通开票服务
     * @param ids
     * @return
     */
    public Boolean invoicing(List<Long> ids) {
        if(CollUtil.isEmpty(ids)){
            return Boolean.FALSE;
        }
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsInvoicing, GlobalConstant.Number.ONE_INT)
                .in(CustomerCompany::getId, ids)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 取消开票服务
     * @param ids
     * @return
     */
    public Boolean cancelInvoicing(List<Long> ids) {
        if(CollUtil.isEmpty(ids)){
            return Boolean.FALSE;
        }
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsInvoicing, GlobalConstant.Number.ZERO_INT)
                .in(CustomerCompany::getId, ids)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 新增公司
     *
     * @param req
     * @param tenantId
     * @param loginUser
     * @return
     */
    /**
     * 小程序新增公司：按 openId 定位 customer_info，再复用 PC 新增逻辑
     */
    public CustomerCompany uaAddCompany(com.qykj.finmaster.customer.company.domain.req.UaAddCompanyReq req) {
        // 1) 按 openId 找 customer_info
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getOpenId, req.getOpenId())
                .one();
        if (ObjectUtils.isEmpty(customerInfo)) {
            throw new com.qykj.finmaster.common.exception.BusinessException("未找到该 openId 对应的客户，请先注册");
        }

        // 2) 校验：同 openId + 同税号 已存在时直接返回已存在记录
        CustomerCompany existed = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getOpenid, req.getOpenId())
                .eq(CustomerCompany::getTaxNumber, req.getTaxNumber())
                .one();
        if (ObjectUtils.isNotEmpty(existed)) return existed;

        // 3) 复制字段
        CustomerCompany customerCompany = CopierCopyUtils.convertTo(req, CustomerCompany.class);
        customerCompany.setCustomerId(customerInfo.getId());
        customerCompany.setOpenid(req.getOpenId());
        customerCompany.setTenantId(customerInfo.getTenantId());

        // 4) 第一家公司默认启用，后续不启用
        LambdaQueryWrapper<CustomerCompany> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(CustomerCompany::getOpenid, req.getOpenId());
        boolean hasAny = customerCompanyManager.getBaseMapper().exists(wrapper);
        customerCompany.setEnabled(hasAny ? GlobalConstant.Number.ZERO_INT : GlobalConstant.Number.ONE_INT);
        customerCompany.setIsVerifyTax(GlobalConstant.Number.ZERO_INT);
        customerCompany.setIsInvoicing(GlobalConstant.Number.ZERO_INT);

        // 5) 记录创建人：小程序用户
        customerCompany.setCreateBy(req.getOpenId());
        customerCompany.setCreateByName("小程序用户");
        customerCompany.setUpdateBy(req.getOpenId());
        customerCompany.setUpdateByName("小程序用户");

        // 6) 入库前防御 + 自动识别 province / taxDistrict（前端没传时）
        sanitizeDirtyStrings(customerCompany);
        ProvinceParser.fillIfBlank(customerCompany);

        customerCompanyManager.save(customerCompany);
        return customerCompany;
    }

    public CustomerCompany addCustomerCompany(AddCustomerCompanyReq req, Long tenantId, LoginUser loginUser) {
        CustomerCompany customerCompany = CopierCopyUtils.convertTo(req, CustomerCompany.class);
        // 设置租户ID
        customerCompany.setTenantId(tenantId);
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getId, customerCompany.getCustomerId())
                .one();
        if (ObjectUtils.isNotEmpty(customerInfo) && ObjectUtils.isNotEmpty(customerInfo.getOpenId())) {
            customerCompany.setOpenid(customerInfo.getOpenId());
        }else{
            // 管理员权限 临时放开
            /*if(!loginUser.isAdmin()) {
                throw new BusinessException(ResultMsg.CUSTOMER_COMPANY_ADD_NOT_OPENID);
            }*/
        }
        customerInfo.setCreateBy(loginUser.getUsername());
        customerInfo.setCreateByName(loginUser.getRealName());
        //是否启用
        LambdaQueryWrapper<CustomerCompany> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(CustomerCompany::getCustomerId, req.getCustomerId());
        if(customerCompanyManager.getBaseMapper().exists(wrapper)){
            customerCompany.setEnabled(GlobalConstant.Number.ZERO_INT);
        }else{
            customerCompany.setEnabled(GlobalConstant.Number.ONE_INT);
        }
        //验税
        customerCompany.setIsVerifyTax(GlobalConstant.Number.ZERO_INT);
        //发票服务
        customerCompany.setIsInvoicing(GlobalConstant.Number.ZERO_INT);
        // 入库前防御 + 自动识别 province / taxDistrict（前端没传时）
        sanitizeDirtyStrings(customerCompany);
        ProvinceParser.fillIfBlank(customerCompany);
        customerCompanyManager.save(customerCompany);
        return customerCompany;
    }

    /**
     * 按税号更新公司基本信息（PC 端，租户隔离）
     * 只覆盖 req 中非 null 的字段；找不到记录 / 多条匹配都报业务异常
     */
    public Boolean updateByTaxNumber(UpdateCustomerCompanyByTaxNumberReq req, Long tenantId, LoginUser loginUser) {
        if (StringUtils.isBlank(req.getTaxNumber())) {
            throw new BusinessException("公司税号不能为空");
        }
        List<CustomerCompany> matched = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getTaxNumber, req.getTaxNumber())
                .eq(CustomerCompany::getTenantId, tenantId)
                .list();
        if (CollUtil.isEmpty(matched)) {
            throw new BusinessException("未找到税号对应的公司");
        }
        if (matched.size() > 1) {
            throw new BusinessException("税号匹配到多条公司记录，请用 id 更新");
        }
        CustomerCompany db = matched.get(0);
        applyUpdatableFields(db, req);
        recalcProvinceOnAddressChange(db, req);
        sanitizeDirtyStrings(db);
        db.setUpdateBy(loginUser.getUsername());
        db.setUpdateByName(loginUser.getRealName());
        db.setUpdateTime(new Date());
        customerCompanyManager.updateById(db);
        return Boolean.TRUE;
    }

    /**
     * 小程序按税号更新公司基本信息
     * 校验：customer_company.openid = openId 且 tax_number = req.taxNumber 才允许
     */
    public Boolean uaUpdateByTaxNumber(UaUpdateCustomerCompanyByTaxNumberReq req) {
        if (StringUtils.isBlank(req.getOpenId())) {
            throw new BusinessException("openId 不能为空");
        }
        if (StringUtils.isBlank(req.getTaxNumber())) {
            throw new BusinessException("公司税号不能为空");
        }
        CustomerCompany db = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getOpenid, req.getOpenId())
                .eq(CustomerCompany::getTaxNumber, req.getTaxNumber())
                .one();
        if (db == null) {
            throw new BusinessException("无权限或公司不存在");
        }
        // 用一个轻量 req 复用 applyUpdatableFields
        UpdateCustomerCompanyByTaxNumberReq pc = new UpdateCustomerCompanyByTaxNumberReq();
        pc.setTaxNumber(req.getTaxNumber());
        pc.setCompanyName(req.getCompanyName());
        pc.setCompanyType(req.getCompanyType());
        pc.setLegalName(req.getLegalName());
        pc.setLegalIdCard(req.getLegalIdCard());
        pc.setLegalPhone(req.getLegalPhone());
        pc.setEstablishedDate(req.getEstablishedDate());
        pc.setRegStatus(req.getRegStatus());
        pc.setRegisteredAddress(req.getRegisteredAddress());
        pc.setTaxDistrict(req.getTaxDistrict());
        pc.setProvince(req.getProvince());
        pc.setIsInvoicing(req.getIsInvoicing());
        pc.setIsIndividual(req.getIsIndividual());
        applyUpdatableFields(db, pc);
        recalcProvinceOnAddressChange(db, pc);
        sanitizeDirtyStrings(db);
        db.setUpdateBy(req.getOpenId());
        db.setUpdateByName("小程序用户");
        db.setUpdateTime(new Date());
        customerCompanyManager.updateById(db);
        // 家族广播：小程序填的工商档案要让 PC 端兄弟 CC 也看到
        try {
            java.util.Map<String, Object> dirty = new java.util.LinkedHashMap<>();
            if (req.getCompanyName() != null)        dirty.put("companyName", req.getCompanyName());
            if (req.getCompanyType() != null)        dirty.put("companyType", req.getCompanyType());
            if (req.getLegalName() != null)          dirty.put("legalName", req.getLegalName());
            if (req.getLegalIdCard() != null)        dirty.put("legalIdCard", req.getLegalIdCard());
            if (req.getLegalPhone() != null)         dirty.put("legalPhone", req.getLegalPhone());
            if (req.getEstablishedDate() != null)    dirty.put("establishedDate", req.getEstablishedDate());
            if (req.getRegStatus() != null)          dirty.put("regStatus", req.getRegStatus());
            if (req.getRegisteredAddress() != null)  dirty.put("registeredAddress", req.getRegisteredAddress());
            if (req.getTaxDistrict() != null)        dirty.put("taxDistrict", req.getTaxDistrict());
            if (req.getProvince() != null)           dirty.put("province", req.getProvince());
            if (req.getIsIndividual() != null)       dirty.put("isIndividual", req.getIsIndividual());
            if (req.getIsInvoicing() != null)        dirty.put("isInvoicing", req.getIsInvoicing());
            if (!dirty.isEmpty()) {
                customerCompanyBroadcaster.broadcast(db.getId(), dirty, req.getOpenId());
            }
        } catch (Exception ex) {
            log.warn("[uaUpdateByTaxNumber] 广播失败 cc={} err={}", db.getId(), ex.getMessage());
        }
        return Boolean.TRUE;
    }

    /**
     * 大屏：合作中公司统计（严格口径 = COOPERATING + 含 service_id=1 记账服务，DISTINCT 税号）。
     * 返回 {total, provinces}：total 不分省（对齐权威 SQL 保证 KPI），provinces 有省份供地图。
     */
    public com.qykj.finmaster.customer.company.domain.resp.CompanyScreenStatVo companyProvinceStat() {
        com.qykj.finmaster.customer.company.domain.resp.CompanyScreenStatVo vo =
                new com.qykj.finmaster.customer.company.domain.resp.CompanyScreenStatVo();
        Long total = customerCompanyMapper.coopCompanyTotal();
        vo.setTotal(total == null ? 0L : total);
        vo.setProvinces(customerCompanyMapper.coopCompanyByProvince());
        return vo;
    }

    /** 只覆盖非 null 字段，避免把没传的字段抹掉 */
    private void applyUpdatableFields(CustomerCompany db, UpdateCustomerCompanyByTaxNumberReq req) {
        if (req.getCompanyName() != null) db.setCompanyName(req.getCompanyName());
        if (req.getCompanyType() != null) db.setCompanyType(req.getCompanyType());
        if (req.getLegalName() != null) db.setLegalName(req.getLegalName());
        if (req.getLegalIdCard() != null) db.setLegalIdCard(req.getLegalIdCard());
        if (req.getLegalPhone() != null) db.setLegalPhone(req.getLegalPhone());
        if (req.getEstablishedDate() != null) db.setEstablishedDate(req.getEstablishedDate());
        if (req.getRegStatus() != null) db.setRegStatus(req.getRegStatus());
        if (req.getRegisteredAddress() != null) db.setRegisteredAddress(req.getRegisteredAddress());
        if (req.getTaxDistrict() != null) db.setTaxDistrict(req.getTaxDistrict());
        if (req.getProvince() != null) db.setProvince(req.getProvince());
        if (req.getIsInvoicing() != null) db.setIsInvoicing(req.getIsInvoicing());
        if (req.getIsIndividual() != null) db.setIsIndividual(req.getIsIndividual());
        if (req.getCompanyStatus() != null) db.setCompanyStatus(req.getCompanyStatus());
        if (req.getStatusRemark() != null) db.setStatusRemark(req.getStatusRemark());
    }

    /**
     * 清洗历史脏数据：前端某处可能 String(undefined) 把字面字符串 "undefined" / "null" 入库。
     * 所有 customer_company 写入入口（add / update）保存前都过一遍，确保不会持续脏化。
     */
    private void sanitizeDirtyStrings(CustomerCompany cc) {
        if (cc == null) return;
        cc.setLegalName(blankIfDirty(cc.getLegalName()));
        cc.setLegalIdCard(blankIfDirty(cc.getLegalIdCard()));
        cc.setLegalPhone(blankIfDirty(cc.getLegalPhone()));
        cc.setRegisteredAddress(blankIfDirty(cc.getRegisteredAddress()));
        cc.setTaxDistrict(blankIfDirty(cc.getTaxDistrict()));
        cc.setProvince(blankIfDirty(cc.getProvince()));
        cc.setRegStatus(blankIfDirty(cc.getRegStatus()));
        cc.setStatusRemark(blankIfDirty(cc.getStatusRemark()));
    }

    private static String blankIfDirty(String v) {
        if (v == null) return null;
        if ("undefined".equals(v) || "null".equals(v)) return null;
        return v;
    }

    /**
     * 注册地址有变更时，对未显式传值的 province / taxDistrict 按新地址自动识别
     * - req.registeredAddress != null：说明前端要更新地址
     * - req.province == null：前端没显式覆盖 → 按新地址重算
     * - req.taxDistrict == null：同上
     */
    private void recalcProvinceOnAddressChange(CustomerCompany db, UpdateCustomerCompanyByTaxNumberReq req) {
        if (StringUtils.isBlank(req.getRegisteredAddress())) return;
        String addr = db.getRegisteredAddress();
        if (req.getProvince() == null) {
            String parsed = ProvinceParser.parseProvince(addr);
            if (StringUtils.isNotBlank(parsed)) db.setProvince(parsed);
        }
        if (req.getTaxDistrict() == null) {
            String parsed = ProvinceParser.parseTaxDistrict(addr);
            if (StringUtils.isNotBlank(parsed)) db.setTaxDistrict(parsed);
        }
    }
}
