package com.qykj.finmaster.customer.customerInfoLog.service;

import java.util.*;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.customer.customerInfoLog.domain.req.AddCustomerLogReq;
import com.qykj.finmaster.customer.customerInfoLog.domain.req.GetCustomerLogPageReq;
import com.qykj.finmaster.customer.customerInfoLog.domain.req.UpdateCustomerLogReq;
import com.qykj.finmaster.customer.customerInfoLog.domain.resp.GetCustomerLogPageResp;
import com.qykj.finmaster.customer.customerInfoLog.domain.resp.GetCustomerLogResp;
import com.qykj.finmaster.customer.customerInfoLog.manager.impl.CustomerLogManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
import com.qykj.finmaster.customer.customerInfoLog.mapper.CustomerLogMapper;
import com.qykj.finmaster.customer.customerInfoLog.domain.CustomerLog;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户操作日志Service业务层处理
 */
@Service
public class CustomerLogServiceImpl {

    @Autowired
    private CustomerLogMapper customerLogMapper;
    @Autowired
    private CustomerLogManagerImpl customerLogManager;

    /**
     * 查询客户操作日志
     */
    public GetCustomerLogResp selectCustomerLogById(Long id) {
        CustomerLog customerLog = customerLogManager.lambdaQuery()
                .eq(CustomerLog::getId, id)
                .one();
        return CopierCopyUtils.convertTo(customerLog, GetCustomerLogResp.class);
    }

    /**
     * 查询客户操作日志列表
     *
     * 数据权限：customer_log.create_by 存的是操作人 userId（非 username），
     * 用 LoginUser.dataScopeUserList（List<Long>）匹配。
     *   - 超管 或 dataScopeUserList=null（全部数据权限）→ 不过滤
     *   - dataScopeUserList 空 → 无可见数据，直接返回空
     *   - 否则把 userId 列表注入 params.dataScopeUserIdList，由 XML foreach 过滤
     */
    public List<GetCustomerLogPageResp> selectCustomerLogList(GetCustomerLogPageReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();

        LoginUser loginUser = getCurrentLoginUser();
        if (loginUser != null && !loginUser.isAdmin()) {
            List<Long> scope = loginUser.getDataScopeUserList();
            if (scope != null) {
                if (scope.isEmpty()) {
                    return Collections.emptyList();
                }
                params.put("dataScopeUserIdList", scope);
            }
        }
        // 移除 PageEntity 默认注入的按 username 过滤字段（本表用 userId 过滤）
        params.remove("dataScopeUserIds");

        List<GetCustomerLogPageResp> list = customerLogMapper.selectCustomerLogList(params);
        if(CollUtil.isEmpty(list)){
            return Collections.emptyList();
        }
        return list;
    }

    private LoginUser getCurrentLoginUser() {
        try {
            org.springframework.security.core.Authentication auth =
                    SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof LoginUser) {
                return (LoginUser) auth.getPrincipal();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 新增客户操作日志
     */
    public Boolean insertCustomerLog(AddCustomerLogReq req, Long tenantId, LoginUser loginUser) {
        CustomerLog customerLog = CopierCopyUtils.convertTo(req, CustomerLog.class);
        customerLog.setTenantId(tenantId);
        if(ObjectUtils.isNotEmpty(loginUser)) {
            customerLog.setCreateBy(loginUser.getUsername());
            customerLog.setCreateByName(loginUser.getRealName());
        }
        customerLog.setCreateTime(new Date());
        customerLogManager.save(customerLog);
        return Boolean.TRUE;
    }

    /**
     * 修改客户操作日志
     */
    public Boolean updateCustomerLog(UpdateCustomerLogReq req, Long tenantId, LoginUser loginUser) {
        CustomerLog customerLog = CopierCopyUtils.convertTo(req, CustomerLog.class);
        if(ObjectUtils.isNotEmpty(loginUser)){
            customerLog.setUpdateBy(loginUser.getUsername());
            customerLog.setUpdateByName(loginUser.getRealName());
        }
        customerLog.setTenantId(tenantId);
        customerLog.setUpdateTime(new Date());
        customerLogManager.updateById(customerLog);
        return Boolean.TRUE;
    }

    /**
     * 批量删除客户操作日志
     */
    public Boolean deleteCustomerLogByIds(String ids) {
        if(StringUtils.isEmpty(ids)){
            return Boolean.FALSE;
        }
        List<Long> idList = Arrays.asList(ids.split(GlobalConstant.Symbol.COMMA)).stream()
                .map(Long::valueOf).toList();
        int rows = customerLogMapper.deleteBatchIds(idList);
        return Boolean.TRUE;
    }

    /**
     * 删除客户操作日志信息
     */
    public int deleteCustomerLogById(Long id) {
        try {

            int rows = customerLogMapper.deleteCustomerLogById(id);
            return rows;
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * 记录客户操作日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordCustomerLog(Long customerId, Integer customerType, Integer logType,
                                  String content, String followType, Date nextFollowTime, Long tenantId) {
        CustomerLog log = new CustomerLog();
        log.setCustomerId(customerId);
        // 临时注释掉customerType设置，直到数据库表结构更新
        // log.setCustomerType(customerType); // 设置客户类型
        log.setLogType(logType);
        log.setContent(content);
        log.setFollowType(followType);
        log.setNextFollowTime(nextFollowTime);
        log.setCreateBy(SecurityContextHolder.getContext().getAuthentication().getName());
        log.setCreateTime(new Date());
        log.setTenantId(tenantId);
        customerLogManager.save(log);
    }

    /**
     * 记录客户操作日志
     */
    @Transactional(rollbackFor = Exception.class)
    public void recordCustomerLog(Long customerId, Integer logType, String content, String createBy, String createByName) {
        CustomerLog log = new CustomerLog();
        log.setCustomerId(customerId);
        // 临时注释掉customerType设置，直到数据库表结构更新
        // log.setCustomerType(customerType); // 设置客户类型
        log.setLogType(logType);
        log.setContent(content);
        log.setCreateBy(createBy);
        log.setCreateByName(createByName);
        log.setCreateTime(new Date());
        customerLogManager.save(log);
    }
}
