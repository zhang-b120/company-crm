package com.qykj.finmaster.customer.customerInfo.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.LogMessage;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.domain.req.*;
import com.qykj.finmaster.customer.customerInfo.domain.resp.GetCustomerInfoResp;
import com.qykj.finmaster.customer.customerInfo.domain.resp.GetCustomerInfoPageListResp;
import com.qykj.finmaster.customer.customerInfo.domain.resp.UaGetCustomerInfoResp;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.customer.customerInfo.mapper.CustomerInfoMapper;
import com.qykj.finmaster.customer.customerInfoLog.domain.CustomerLog;
import com.qykj.finmaster.customer.customerInfoLog.manager.impl.CustomerLogManagerImpl;
import com.qykj.finmaster.customer.customerInfoLog.service.CustomerLogServiceImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.customer.leadLog.entity.LeadLog;
import com.qykj.finmaster.customer.leadLog.manager.impl.LeadLogManagerImpl;
import com.qykj.finmaster.customer.leadLog.service.LeadLogServiceImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 客户信息Service业务层处理
 */
@Service
@Slf4j
public class CustomerInfoServiceImpl {

    @Autowired
    private CustomerInfoMapper customerInfoMapper;

    @Autowired
    private CustomerLogServiceImpl customerLogService;

    @Autowired
    private CustomerInfoManagerImpl customerInfoManager;

    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;

    @Autowired
    private SysDictDataServiceImpl sysDictDataService;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private OrderManagerImpl orderManager;
    @Autowired
    private CustomerLogManagerImpl customerLogManager;
    @Autowired
    private LeadManagerImpl leadManager;
    @Autowired
    private LeadLogServiceImpl leadLogService;
    @Autowired
    private LeadLogManagerImpl leadLogManager;

    /**
     * 查询客户信息
     */
    public GetCustomerInfoResp selectCustomerInfoById(Long id) {
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getId, id)
                .one();
        return CopierCopyUtils.convertTo(customerInfo, GetCustomerInfoResp.class);
    }

    /**
     * 客户全生命周期时间轴
     * 合并两类日志：
     *   - 线索阶段（该客户 leadId 对应的 LeadLog）
     *   - 客户阶段（该客户 customerId 对应的 CustomerLog）
     * 按时间倒序
     *
     * 返回 List<Map>：{ stage, logType, content, nextFollowTime, followMethod, operator, time }
     *   stage: "lead" | "customer"
     *   logType: 1 创建 / 2 跟进 / 3 分配 / 4 转化 / 5 有效性标记
     */
    public List<Map<String, Object>> selectCustomerTimeline(Long customerId) {
        List<Map<String, Object>> timeline = new ArrayList<>();

        CustomerInfo customer = customerInfoManager.getById(customerId);
        if (customer == null) return timeline;

        // 1) 线索阶段：customer.leadId → LeadLog
        if (customer.getLeadId() != null) {
            List<LeadLog> leadLogs = leadLogManager.lambdaQuery()
                    .eq(LeadLog::getLeadId, customer.getLeadId())
                    .orderByDesc(LeadLog::getCreateTime)
                    .list();
            for (LeadLog l : leadLogs) {
                Map<String, Object> item = new HashMap<>();
                item.put("stage", "lead");
                item.put("logType", l.getLogType());
                item.put("content", l.getContent());
                item.put("nextFollowTime", l.getNextFollowTime());
                item.put("followMethod", l.getFollowMethod());
                item.put("operator", l.getOperatorName() != null ? l.getOperatorName() : l.getCreateByName());
                item.put("time", l.getCreateTime());
                timeline.add(item);
            }
        }

        // 2) 客户阶段：customerId → CustomerLog
        List<CustomerLog> customerLogs = customerLogManager.lambdaQuery()
                .eq(CustomerLog::getCustomerId, customerId)
                .orderByDesc(CustomerLog::getCreateTime)
                .list();
        for (CustomerLog l : customerLogs) {
            Map<String, Object> item = new HashMap<>();
            item.put("stage", "customer");
            item.put("logType", l.getLogType());
            item.put("content", l.getContent());
            item.put("nextFollowTime", l.getNextFollowTime());
            item.put("followMethod", l.getFollowType());
            item.put("operator", l.getCreateByName());
            item.put("time", l.getCreateTime());
            timeline.add(item);
        }

        // 3) 按时间倒序合并
        timeline.sort((a, b) -> {
            Date t1 = (Date) a.get("time");
            Date t2 = (Date) b.get("time");
            if (t1 == null && t2 == null) return 0;
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });

        return timeline;
    }

    /**
     * 查询客户信息列表
     */
    public List<GetCustomerInfoPageListResp> selectCustomerInfoList(GetCustomerInfoReq getCustomerInfoReq, Long tenantId) {
        Map<String, Object> params = getCustomerInfoReq.toBeanMap();
        // 设置租户ID过滤条件
        params.put("tenantId", tenantId);
        if(ObjectUtils.isNotEmpty(getCustomerInfoReq.getFollowCount())){
            params.put("isTransaction", false);
        }
        List<GetCustomerInfoPageListResp> list = customerInfoMapper.selectCustomerInfoList(params);
        if(CollUtil.isEmpty(list)){
            return Collections.emptyList();
        }
        //获取客户已支付订单
        List<Long> customerIds = list.stream().map(GetCustomerInfoPageListResp::getId).toList();
        List<Long> transactionList = getTransactionCustomerIds(customerIds, tenantId);
        //客户日志
        List<CustomerLog> customerLogList = customerLogManager.lambdaQuery()
                .in(CustomerLog::getCustomerId, customerIds)
                .eq(CustomerLog::getLogType, GlobalConstant.Number.TWO_INT)
                .list();
        Map<Long, List<CustomerLog>> customerLogCollect = new HashMap<>();
        if(CollUtil.isNotEmpty(customerLogList)){
            customerLogCollect = customerLogList.stream().collect(Collectors.groupingBy(CustomerLog::getCustomerId));
        }
        Map<Long, List<CustomerLog>> finalCustomerLogCollect = customerLogCollect;
        list.forEach(item -> {
            if(transactionList.contains(item.getId())){
                item.setIsTransaction(Boolean.TRUE);
            }else {
                item.setIsTransaction(Boolean.FALSE);
                if(ObjectUtils.isNotEmpty(item.getLastFollowTime())){
                    item.setLastFollowInterval(DateUtils.getDaysDifference(LocalDateTime.now(), DateUtils.dateToLocalDateTime(item.getLastFollowTime())));
                }else{
                    item.setLastFollowInterval(DateUtils.getDaysDifference(LocalDateTime.now(), DateUtils.dateToLocalDateTime(item.getCreateTime())));
                }
                if(CollUtil.isNotEmpty(finalCustomerLogCollect) && finalCustomerLogCollect.containsKey(item.getId())){
                    item.setFollowCount(finalCustomerLogCollect.get(item.getId()).size());
                }else{
                    item.setFollowCount(GlobalConstant.Number.ZERO_INT);
                }

            }
        });
        return list;
    }

    /**
     * 获取已成交客户
     * @return
     */
    public List<Long> getTransactionCustomerIds(List<Long> customerIds, Long tenantId){
        if (CollUtil.isEmpty(customerIds) || tenantId == null) {
            return Collections.emptyList();
        }
        Set<Long> transactionCustomerSet = new HashSet<>();
        // 现根据是否存在有效订单然后再判断客户的公司是否已合作
        List<Long> orderCustomerIds = getCustomerOrderList(customerIds, tenantId);
        if(CollUtil.isNotEmpty(orderCustomerIds)){
            transactionCustomerSet.addAll(orderCustomerIds);
        }
        List<Long> companyCustomerIds = getCustomerCompanyList(customerIds, tenantId);
        if(CollUtil.isNotEmpty(companyCustomerIds)){
            transactionCustomerSet.addAll(companyCustomerIds);
        }
        return new ArrayList<>(transactionCustomerSet);
    }

    /**
     * 获取存在有效订单客户
     * @param customerIds
     * @return
     */
    private List<Long> getCustomerOrderList(List<Long> customerIds, Long tenantId){
        if(CollUtil.isEmpty(customerIds)){
            return Collections.emptyList();
        }
        //查询客户相关订单信息
        List<Order> orderList = orderManager.lambdaQuery()
                .in(Order::getCustomerId, customerIds)
                .eq(Order::getStatus, DictConstants.ORDER_STATUS_PAID)
                .eq(Order::getOrderType, DictConstants.ORDER_TYPE_NORMAL)
                .eq(Order::getTenantId, tenantId)
                .list();

        if(CollUtil.isEmpty(orderList)){
            return Collections.emptyList();
        }else{
            return orderList.stream().map(Order::getCustomerId).distinct().toList();
        }
    }

    /**
     * 获取已合作公司的客户
     * @param customerIds
     * @param tenantId
     * @return
     */
    private List<Long> getCustomerCompanyList(List<Long> customerIds, Long tenantId){
        if(CollUtil.isEmpty(customerIds)){
            return Collections.emptyList();
        }
        //查询客户相关公司信息
        List<CustomerCompany> customerCompanyList = customerCompanyManager.lambdaQuery()
                .in(CustomerCompany::getCustomerId, customerIds)
                .and(item -> item.eq(CustomerCompany::getIsVerifyTax, GlobalConstant.Number.ONE_INT)
                        .or().eq(CustomerCompany::getIsInvoicing, GlobalConstant.Number.ONE_INT))
                .eq(CustomerCompany::getTenantId, tenantId)
                .list();

        if(CollUtil.isEmpty(customerCompanyList)){
            return Collections.emptyList();
        }else{
            return customerCompanyList.stream().map(CustomerCompany::getCustomerId).distinct().toList();
        }
    }

    /**
     * 新增客户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertCustomerInfo(AddCustomerInfoReq req, Long tenantId, LoginUser loginUser) {
        CustomerInfo customerInfo = CopierCopyUtils.convertTo(req, CustomerInfo.class);
        customerInfo.setCreateBy(loginUser.getUsername());
        customerInfo.setCreateByName(loginUser.getRealName());
        customerInfo.setTenantId(tenantId); // 设置租户ID

        // 校验手机号是否重复
        if (StringUtils.isNotEmpty(req.getPhone())) {
            boolean exists = customerInfoManager.lambdaQuery()
                    .eq(CustomerInfo::getPhone, req.getPhone())
                    .eq(CustomerInfo::getTenantId, tenantId)
                    .exists();
            if (exists) {
                throw new BusinessException("手机号 " + req.getPhone() + " 已存在");
            }
        }

        // 根据手机号查询线索，关联leadId和openId
        if (StringUtils.isNotEmpty(req.getPhone()) && req.getLeadId() == null) {
            Lead lead = leadManager.lambdaQuery()
                    .eq(Lead::getPhone, req.getPhone())
                    .eq(Lead::getTenantId, tenantId)
                    .last("LIMIT 1")
                    .one();
            if (ObjectUtils.isNotEmpty(lead)) {
                customerInfo.setLeadId(lead.getId());
                customerInfo.setLeadName(lead.getLeadName());
                if (StringUtils.isNotEmpty(lead.getOpenId())) {
                    customerInfo.setOpenId(lead.getOpenId());
                }
            }
        }

        // 如果没有指定负责人，则设置为当前登录用户
        if (StringUtils.isEmpty(customerInfo.getOwnerUser())) {
            customerInfo.setOwnerUser(loginUser.getUsername());
        }
        if (StringUtils.isEmpty(customerInfo.getOwnerUserName())) {
            customerInfo.setOwnerUserName(loginUser.getRealName());
        }
        boolean flag = customerInfoManager.save(customerInfo);
        if (flag) {
            // 记录创建日志
            customerLogService.recordCustomerLog(customerInfo.getId(), GlobalConstant.Number.ONE_INT,  GlobalConstant.Number.ONE_INT,
                    "创建客户：" + customerInfo.getCustomerName(), null, null, tenantId);
            // 同步到外部系统
            //syncToExternalSystem(customerInfo);
        }
        return Boolean.TRUE;
    }

    /**
     * 修改客户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCustomerInfo(UpdateCustomerInfoReq updateCustomerInfoReq, Long tenantId, LoginUser loginUser) {
        CustomerInfo customerInfo = CopierCopyUtils.convertTo(updateCustomerInfoReq, CustomerInfo.class);
        if (ObjectUtils.isNotEmpty(loginUser)) {
            customerInfo.setUpdateBy(loginUser.getUsername());
            customerInfo.setUpdateByName(loginUser.getRealName());
        }
        customerInfo.setTenantId(tenantId); // 确保租户ID不被修改

        // 校验手机号是否重复（排除自身）
        if (StringUtils.isNotEmpty(updateCustomerInfoReq.getPhone())) {
            boolean exists = customerInfoManager.lambdaQuery()
                    .eq(CustomerInfo::getPhone, updateCustomerInfoReq.getPhone())
                    .eq(CustomerInfo::getTenantId, tenantId)
                    .ne(CustomerInfo::getId, updateCustomerInfoReq.getId())
                    .exists();
            if (exists) {
                throw new BusinessException("手机号 " + updateCustomerInfoReq.getPhone() + " 已存在");
            }
        }

        customerInfo.setUpdateTime(new Date());
        customerInfoManager.getBaseMapper().updateById(customerInfo);
            // 记录修改日志
        customerLogService.recordCustomerLog(customerInfo.getId(), GlobalConstant.Number.ONE_INT, GlobalConstant.Number.FOUR_INT,
                "修改客户信息：" + customerInfo.getCustomerName(), null, null, tenantId);
        return Boolean.TRUE;
    }

    /**
     * 批量删除客户信息
     */
    public Boolean deleteCustomerInfoByIds(String ids) {
        if(StringUtils.isEmpty(ids)){
            return Boolean.FALSE;
        }
        List<Long> list = Arrays.asList(ids.split(GlobalConstant.Symbol.COMMA)).stream()
                .map(Long::valueOf).toList();
        customerInfoMapper.deleteBatchIds(list);
        return Boolean.TRUE;
    }

    /**
     * 删除客户信息
     */
    public Boolean deleteCustomerInfoById(Long id) {
        customerInfoMapper.deleteById(id);
        return Boolean.TRUE;
    }

    /**
     * 公海批量废弃：对所选客户做逻辑删除（deleted=1）
     * 限制：只允许废弃公海客户（customerType=2），避免误删私海在跟进的数据
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean discardPublicSeaCustomers(List<Long> ids, Long tenantId, LoginUser loginUser) {
        if (CollUtil.isEmpty(ids)) {
            return Boolean.FALSE;
        }
        List<CustomerInfo> list = customerInfoManager.lambdaQuery()
                .in(CustomerInfo::getId, ids)
                .list();
        if (CollUtil.isEmpty(list)) {
            return Boolean.FALSE;
        }
        // 任何一条不是公海都拒绝整批，避免前端状态不同步导致误删
        List<String> invalid = list.stream()
                .filter(c -> c.getCustomerType() == null || c.getCustomerType() != GlobalConstant.Number.TWO_INT)
                .map(CustomerInfo::getCustomerName)
                .collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            throw new BusinessException("只能废弃公海客户，以下不是公海客户：" + String.join("、", invalid));
        }

        // 逻辑删除：MyBatis-Plus 全局 logic-delete-field=deleted，deleteBatchIds 会走软删
        customerInfoMapper.deleteBatchIds(ids);

        // 写日志（logType=8 废弃）
        String operator = loginUser != null ? loginUser.getUsername() : "system";
        String operatorName = loginUser != null ? loginUser.getRealName() : "系统";
        Date now = new Date();
        for (CustomerInfo c : list) {
            try {
                CustomerLog logEntry = new CustomerLog();
                logEntry.setCustomerId(c.getId());
                logEntry.setLogType(GlobalConstant.Number.EIGHT_INT);
                logEntry.setContent("废弃客户：" + c.getCustomerName());
                logEntry.setCreateBy(operator);
                logEntry.setCreateByName(operatorName);
                logEntry.setCreateTime(now);
                logEntry.setTenantId(tenantId);
                customerLogManager.save(logEntry);
            } catch (Exception e) {
                log.warn("[客户废弃] 写日志失败 customerId={}: {}", c.getId(), e.getMessage());
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 转入公海
     */
    @Transactional
    public Boolean transferToPublicSea(Long id, Long tenantId, LoginUser loginUser) {
        // 1. 获取客户信息
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getId, id)
                .one();
        if (ObjectUtils.isEmpty(customerInfo)) {
            return Boolean.FALSE;
        }
        // 2. 更新客户状态为公海客户
        customerInfoManager.lambdaUpdate()
                .set(CustomerInfo::getCustomerType, GlobalConstant.Number.TWO_INT)
                .set(CustomerInfo::getOwnerUser, null)
                .set(CustomerInfo::getOwnerUserName, null)
                .set(CustomerInfo::getUpdateBy, loginUser.getUsername())
                .set(CustomerInfo::getUpdateByName, loginUser.getRealName())
                .set(CustomerInfo::getUpdateTime, new Date())
                .eq(CustomerInfo::getId, id)
                .update();
        // 3. 记录转入公海日志
        customerLogService.recordCustomerLog( id, GlobalConstant.Number.ONE_INT, GlobalConstant.Number.FIVE_INT,
                "客户转入公海：" + customerInfo.getCustomerName(), null, null, tenantId);
        // 释放线索（删除负责人）
        if(ObjectUtils.isNotEmpty(customerInfo.getLeadId())){
            dealLead(customerInfo.getLeadId(), null,null, loginUser, Boolean.TRUE);
        }
        return Boolean.TRUE;
    }

    /**
     * 释放线索
     * @param id
     * @param loginUser 用户
     * @param flag true-转入公海 false-认领客户
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public void dealLead(Long id, String ownerUser, String ownerUserName, LoginUser loginUser, Boolean flag) {
        Lead lead = leadManager.lambdaQuery()
                .eq(Lead::getId, id)
                .one();
        if(ObjectUtils.isNotEmpty(lead)){
            leadManager.lambdaUpdate()
                    .set(Lead::getOwnerUser, ownerUser)
                    .set(Lead::getOwnerUserName, ownerUserName)
                    .set(Lead::getUpdateBy, loginUser.getUsername())
                    .set(Lead::getUpdateByName, loginUser.getRealName())
                    .eq(Lead::getId, id)
                    .update();
            // 日志
            String content = "";
            if(flag){
                content = MessageFormat.format(LogMessage.CUSTOMER_TO_RELEASE_LEAD_LOG, lead.getOwnerUserName(), loginUser.getRealName());
            }else{
                content = MessageFormat.format(LogMessage.CUSTOMER_TO_ASSIGN_LEAD_LOG, ownerUserName, loginUser.getRealName());
            }
            leadLogService.addLeadLogByCommon(lead.getId(), GlobalConstant.Number.THREE_INT,
                    content, loginUser.getUserId(), loginUser.getRealName());
        }
    }

    /**
     * 认领客户
     */
    @Transactional
    public Boolean claimCustomer(Long id, Long tenantId) {
        // 1. 获取客户信息
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getId, id)
                .one();
        if (ObjectUtils.isEmpty(customerInfo)) {
            return Boolean.FALSE;
        }
        // 2. 检查客户是否为公海客户
        if (customerInfo.getCustomerType() != GlobalConstant.Number.TWO_INT) {
            return Boolean.FALSE;
        }

        // 3. 获取当前登录用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        // 4. 更新客户为私海客户
        customerInfo.setCustomerType(GlobalConstant.Number.ONE_INT); // 1 表示私海客户
        customerInfo.setOwnerUser(loginUser.getUsername());
        customerInfo.setOwnerUserName(loginUser.getRealName());
        customerInfo.setTenantId(tenantId); // 确保租户ID不被修改
        customerInfoManager.getBaseMapper().updateById(customerInfo);
        // 5. 记录认领日志
        customerLogService.recordCustomerLog(id, GlobalConstant.Number.ONE_INT,  GlobalConstant.Number.SIX_INT,
                "客户被认领：" + customerInfo.getCustomerName() + "，认领人：" + loginUser.getRealName(),
                null, null, tenantId);
        // 分配线索
        if(ObjectUtils.isNotEmpty(customerInfo.getLeadId())){
            dealLead(customerInfo.getLeadId(), customerInfo.getOwnerUser(), customerInfo.getOwnerUserName(), loginUser, Boolean.FALSE);
        }
        return Boolean.TRUE;

    }

    /**
     * 更新最近跟进信息
     */
    public int updateLastFollowInfo(Long id, String content, Date nextFollowTime, String status, Long tenantId) {
        CustomerInfo customerInfo = new CustomerInfo();
        customerInfo.setId(id);
        customerInfo.setLastFollowContent(content);
        customerInfo.setLastFollowTime(new Date());
        customerInfo.setNextFollowTime(nextFollowTime);
        customerInfo.setTenantId(tenantId); // 设置租户ID
        customerInfo.setStatus(Integer.valueOf(status));
        return customerInfoManager.getBaseMapper().updateById(customerInfo);
    }

    public UaGetCustomerInfoResp getCustomerInfo(UaGetCustomerInfoReq req) {
        UaGetCustomerInfoResp resp = new UaGetCustomerInfoResp();
        if (StringUtils.isEmpty(req.getOpenId())) {
            return resp;
        }

        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getOpenId, req.getOpenId())
                .one();
        if (ObjectUtils.isNotEmpty(customerInfo)) {
            resp.setName(customerInfo.getCustomerName());
        }
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getOpenid, req.getOpenId())
                .eq(CustomerCompany::getEnabled, GlobalConstant.Number.ONE_INT)
                .one();
        if (ObjectUtils.isNotEmpty(customerCompany)) {
            resp.setCreditCode(customerCompany.getTaxNumber());
            if (StringUtils.isNotEmpty(resp.getCreditCode())) {
                JSONObject dataJson = FinancialUtils.getAccountSets(resp.getCreditCode());
                if (ObjectUtils.isNotEmpty(dataJson)) {
                    resp.setAccountSetsId(dataJson.getLong("id"));
                    resp.setCurrentAccountDate(dataJson.getString("currentAccountDate"));
                    resp.setEnableDate(dataJson.getString("enableDate"));
                }
            }
        }
        return resp;
    }

    /**
     * 同步到外部系统
     */
    private void syncToExternalSystem(Map<String, Object> getCustomerInfoReqMap) {
        try {
            // 获取MES_FLAG字典值
            String mesFlag = sysDictDataService.selectDictDataByTypeAndLabel("OUT_SYSTEM", "MES_FLAG").getValue();
            if ("开启".equals(mesFlag)) {
                // 获取MES_CUSTOMER_URL字典值
                String mesUrl = sysDictDataService.selectDictDataByTypeAndLabel("OUT_SYSTEM", "MES_URL").getValue() + "masterdata/customer/addCustomer4Out";
                if (org.springframework.util.StringUtils.hasText(mesUrl)) {
                    // 构建请求参数
                    Map<String, String> params = new HashMap<>();

                    // 将客户名称转换为拼音首字母
                    String customerName = String.valueOf(getCustomerInfoReqMap.get("customerName"));
                    String pinyinNumber = "";
                    if (StringUtils.isNotEmpty(customerName)) {
                        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
                        format.setCaseType(HanyuPinyinCaseType.UPPERCASE);
                        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);

                        StringBuilder pinyinBuilder = new StringBuilder();
                        for (char c : customerName.toCharArray()) {
                            if (Character.toString(c).matches("[\\u4E00-\\u9FA5]+")) {
                                try {
                                    String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(c, format);
                                    if (pinyinArray != null && pinyinArray.length > 0) {
                                        pinyinBuilder.append(pinyinArray[0].charAt(0));
                                    }
                                } catch (BadHanyuPinyinOutputFormatCombination e) {
                                    log.error("转换拼音失败", e);
                                }
                            } else {
                                pinyinBuilder.append(c);
                            }
                        }
                        pinyinNumber = pinyinBuilder.toString();
                    }

                    // 使用拼音首字母作为number
                    params.put("number", pinyinNumber);
                    params.put("name", customerName);
                    params.put("phone", String.valueOf(getCustomerInfoReqMap.get("phone")));
                    params.put("createBy", String.valueOf(getCustomerInfoReqMap.get("createBy")));

                    // 调用外部接口
                    ResponseEntity<String> response = restTemplate.postForEntity(mesUrl, params, String.class);
                    if (response.getStatusCode() != HttpStatus.OK) {
                        log.error("同步客户数据到外部系统失败");
                    }
                }
            }
        } catch (Exception e) {
            log.error("同步客户数据到外部系统异常", e);
            // 这里不抛出异常，保证主流程不受影响
        }
    }

    /**
     * 分配客户
     * @param req
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignCustomerInfo(AssignCustomerInfoReq req, LoginUser loginUser, Long tenantId) {
        if(ObjectUtils.isEmpty(req) || CollUtil.isEmpty(req.getIds())){
            return Boolean.FALSE;
        }
        if(StringUtils.isEmpty(req.getOwnerUser()) || StringUtils.isEmpty(req.getOwnerUserName())){
            return Boolean.FALSE;
        }
        // 管理员权限
        if(!loginUser.isAdmin()){
            throw new BusinessException(ResultMsg.COMMON_NOT_HAVE_ROLE);
        }
        // 获取要分配的客户（公海）
        List<CustomerInfo> customerInfoList = customerInfoManager.lambdaQuery()
                .in(CustomerInfo::getId, req.getIds())
                .eq(CustomerInfo::getCustomerType, GlobalConstant.Number.TWO_INT)
                .isNull(CustomerInfo::getOwnerUserName)
                .isNull(CustomerInfo::getOwnerUser)
                .list();
        if(CollUtil.isNotEmpty(customerInfoList)) {
            //修改
            List<Long> customerIds = customerInfoList.stream().map(CustomerInfo::getId).toList();
            customerInfoManager.lambdaUpdate()
                    .in(CustomerInfo::getId, customerIds)
                    .set(CustomerInfo::getOwnerUser, req.getOwnerUser())
                    .set(CustomerInfo::getOwnerUserName, req.getOwnerUserName())
                    .set(CustomerInfo::getUpdateBy, loginUser.getRealName())
                    .set(CustomerInfo::getUpdateByName, loginUser.getUsername())
                    .set(CustomerInfo::getCustomerType, GlobalConstant.Number.ONE_INT)
                    .update();
            //记录日志
            customerInfoList.forEach(item -> {
                customerLogService.recordCustomerLog(item.getId(), GlobalConstant.Number.ONE_INT, GlobalConstant.Number.SEVEN_INT,
                        loginUser.getRealName() + "分配客户：{" + item.getCustomerName() + "}给{" + req.getOwnerUserName() + "}",
                        null, null, tenantId);
                if(ObjectUtils.isNotEmpty(item.getLeadId())){
                    dealLead(item.getLeadId(), req.getOwnerUser(),req.getOwnerUserName(), loginUser, Boolean.FALSE);
                }
            });
        }
        return Boolean.TRUE;
    }

    /**
     * 释放客户
     * @param customerInfoList
     * @param ownerUser
     * @param ownerUserName
     * @param loginUser
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseCustomer(List<CustomerInfo> customerInfoList, String ownerUser, String ownerUserName, LoginUser loginUser){
        List<Long> idList = customerInfoList.stream().map(CustomerInfo::getId).toList();
        customerInfoManager.lambdaUpdate()
                .set(CustomerInfo::getCustomerType, GlobalConstant.Number.TWO_INT)
                .set(CustomerInfo::getOwnerUser, ownerUser)
                .set(CustomerInfo::getOwnerUserName, ownerUserName)
                .set(CustomerInfo::getUpdateBy, loginUser.getUsername())
                .set(CustomerInfo::getUpdateByName, loginUser.getRealName())
                .set(CustomerInfo::getUpdateTime, new Date())
                .in(CustomerInfo::getId, idList)
                .update();
        // 客户转入公海日志
        customerInfoList.forEach(item -> {
            customerLogService.recordCustomerLog(item.getId(), GlobalConstant.Number.FIVE_INT,
                    MessageFormat.format(LogMessage.RELEASE_CUSTOMER_LOG, item.getOwnerUserName(), loginUser.getRealName()),
                    loginUser.getUsername(), loginUser.getRealName());
            //释放线索
            dealLead(item.getLeadId(), ownerUser, ownerUserName, loginUser, Boolean.TRUE);
        });
    }
}
