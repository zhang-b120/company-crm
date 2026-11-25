package com.qykj.finmaster.customer.leadLog.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.customer.leadLog.entity.LeadLog;
import com.qykj.finmaster.customer.leadLog.entity.req.AddLeadLog;
import com.qykj.finmaster.customer.leadLog.manager.impl.LeadLogManagerImpl;
import com.qykj.finmaster.customer.leadLog.mapper.LeadLogMapper;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.mapper.LeadMapper;
import com.qykj.finmaster.security.LoginUser;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 线索跟进日志Service实现类
 */
@Service
public class LeadLogServiceImpl {


    @Autowired
    private LeadLogMapper leadLogMapper;

    @Autowired
    private LeadManagerImpl leadManager;

    @Autowired
    private LeadLogManagerImpl leadLogManager;

    /**
     * 添加跟进日志
     *
     * @param req 跟进日志信息
     * @param loginUser
     * @return 结果
     */
    @Transactional
    public Boolean addLeadLog(AddLeadLog req, Long tenantId, LoginUser loginUser) {
        LeadLog leadLog = CopierCopyUtils.convertTo(req, LeadLog.class);
        // 设置租户ID
        leadLog.setTenantId(tenantId);
        if (ObjectUtils.isNotEmpty(loginUser)) {
            leadLog.setOperatorId(loginUser.getUserId());
            leadLog.setOperatorName(loginUser.getRealName());
        }
        // 添加跟进日志
        leadLogManager.save(leadLog);
        // 如果是跟进记录类型，更新线索的最近跟进信息
        if (leadLog.getLogType() == 2) { // 2表示跟进记录
            Lead lead = leadManager.lambdaQuery()
                    .eq(Lead::getId, leadLog.getLeadId())
                    .one();
            if(ObjectUtils.isNotEmpty(lead)){
                LambdaUpdateWrapper<Lead> wrapper = Wrappers.lambdaUpdate();
                wrapper.eq(Lead::getId, leadLog.getLeadId())
                        .set(Lead::getLastFollowTime, new Date())
                        .set(Lead::getLastFollowContent, leadLog.getContent())
                        .set(Lead::getNextFollowTime, DateUtils.dateToLocalDateTime(leadLog.getNextFollowTime()));
                if(GlobalConstant.Number.THREE_INT != lead.getStatus()){
                    wrapper.set( Lead::getStatus, GlobalConstant.Number.TWO_INT);
                }
                leadManager.update(wrapper);
            }
        }
        return Boolean.TRUE;

    }

    /**
     * 查询线索的跟进日志列表
     *
     * 数据权限：按当前登录用户的数据范围过滤 operatorId。
     *   - 管理员或「全部数据权限」：不过滤
     *   - 其他：仅看得到的用户 ID 列表内的日志（本部门/自定义/仅本人）
     *   - operatorId 为 null 的记录（历史遗留或系统早期日志）一并过滤掉，避免越权
     *
     * @param leadId 线索ID
     * @return 跟进日志列表
     */
    public List<LeadLog> getLeadLogList(Long leadId, Long tenantId) {
        var q = leadLogManager.lambdaQuery().eq(LeadLog::getLeadId, leadId);

        LoginUser loginUser = getCurrentLoginUser();
        if (loginUser != null && !loginUser.isAdmin()) {
            List<Long> scope = loginUser.getDataScopeUserList();
            // scope == null 表示「全部数据权限」，不过滤
            if (scope != null) {
                if (scope.isEmpty()) {
                    return java.util.Collections.emptyList();
                }
                q.in(LeadLog::getOperatorId, scope);
            }
        }
        return q.orderByDesc(LeadLog::getCreateTime).list();
    }

    private LoginUser getCurrentLoginUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof LoginUser) {
                return (LoginUser) auth.getPrincipal();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Transactional
    public void addLeadLogByCommon(Long leadId, Integer logType, String content) {
        try {
            LeadLog leadLog = new LeadLog();
            leadLog.setLeadId(leadId);
            leadLog.setLogType(logType);
            leadLog.setContent(content);
            leadLog.setCreateTime(new Date());
            leadLogManager.save(leadLog);
        } catch (Exception e) {
            throw e;
        }
    }

    @Transactional
    public void addLeadLogByCommon(Long leadId, Integer logType, String content, Long operatorId, String operatorName) {
        LeadLog leadLog = new LeadLog();
        leadLog.setLeadId(leadId);
        leadLog.setLogType(logType);
        leadLog.setContent(content);
        leadLog.setOperatorId(operatorId);
        leadLog.setOperatorName(operatorName);
        leadLog.setCreateTime(new Date());
        leadLogManager.save(leadLog);
    }

    @Transactional
    public void addLeadLogByCommon(Long leadId, Integer logType, String content, Date nextFollowTime, String followMethod, Long operatorId, String operatorName) {
        LeadLog leadLog = new LeadLog();
        leadLog.setLeadId(leadId);
        leadLog.setLogType(logType);
        leadLog.setContent(content);
        leadLog.setNextFollowTime(nextFollowTime);
        leadLog.setFollowMethod(followMethod);
        leadLog.setOperatorId(operatorId);
        leadLog.setOperatorName(operatorName);
        leadLog.setCreateTime(new Date());
        leadLogManager.save(leadLog);
    }
}
