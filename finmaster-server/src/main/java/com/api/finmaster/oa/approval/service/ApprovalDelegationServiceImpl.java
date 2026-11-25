package com.qykj.finmaster.oa.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.oa.approval.domain.OaApprovalDelegation;
import com.qykj.finmaster.oa.approval.domain.req.AddDelegationReq;
import com.qykj.finmaster.oa.approval.domain.req.GetDelegationListReq;
import com.qykj.finmaster.oa.approval.manager.impl.OaApprovalDelegationManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 审批委托服务实现类
 */
@Slf4j
@Service
public class ApprovalDelegationServiceImpl {

    @Autowired
    private OaApprovalDelegationManagerImpl delegationManager;

    /**
     * 新增委托
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addDelegation(AddDelegationReq req, Long tenantId, LoginUser loginUser) {
        // 校验日期
        Date startDate = parseDate(req.getStartDate());
        Date endDate = parseDate(req.getEndDate());
        if (endDate.before(startDate)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }

        OaApprovalDelegation delegation = new OaApprovalDelegation();
        delegation.setTenantId(tenantId);
        delegation.setDelegator(loginUser.getUsername());
        delegation.setDelegatorName(loginUser.getRealName());
        delegation.setDelegateTo(req.getDelegateTo());
        delegation.setDelegateToName(req.getDelegateToName());
        delegation.setStartDate(startDate);
        delegation.setEndDate(endDate);
        delegation.setBusinessTypes(req.getBusinessTypes());
        delegation.setEnabled(req.getEnabled() != null ? req.getEnabled() : 1);
        delegation.setCreateTime(new Date());
        delegation.setUpdateTime(new Date());
        delegationManager.save(delegation);

        log.info("新增审批委托，委托人: {}，被委托人: {}，ID: {}", loginUser.getUsername(), req.getDelegateTo(), delegation.getId());
        return delegation.getId();
    }

    /**
     * 更新委托
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateDelegation(AddDelegationReq req, LoginUser loginUser) {
        if (req.getId() == null) {
            throw new BusinessException("委托ID不能为空");
        }
        OaApprovalDelegation delegation = delegationManager.getById(req.getId());
        if (delegation == null) {
            throw new BusinessException("委托记录不存在");
        }
        if (!loginUser.getUsername().equals(delegation.getDelegator())) {
            throw new BusinessException("只能修改自己的委托");
        }

        if (req.getDelegateTo() != null) {
            delegation.setDelegateTo(req.getDelegateTo());
        }
        if (req.getDelegateToName() != null) {
            delegation.setDelegateToName(req.getDelegateToName());
        }
        if (req.getStartDate() != null) {
            delegation.setStartDate(parseDate(req.getStartDate()));
        }
        if (req.getEndDate() != null) {
            delegation.setEndDate(parseDate(req.getEndDate()));
        }
        // 二次校验日期
        if (delegation.getEndDate().before(delegation.getStartDate())) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        if (req.getBusinessTypes() != null) {
            delegation.setBusinessTypes(req.getBusinessTypes());
        }
        if (req.getEnabled() != null) {
            delegation.setEnabled(req.getEnabled());
        }
        delegation.setUpdateTime(new Date());
        delegationManager.updateById(delegation);

        log.info("更新审批委托，ID: {}，操作人: {}", req.getId(), loginUser.getUsername());
    }

    /**
     * 删除委托
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDelegation(Long id) {
        delegationManager.removeById(id);
        log.info("删除审批委托，ID: {}", id);
    }

    /**
     * 查询当前用户的委托列表
     */
    public List<OaApprovalDelegation> getMyDelegations(GetDelegationListReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaApprovalDelegation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaApprovalDelegation::getTenantId, tenantId);
        wrapper.eq(OaApprovalDelegation::getDelegator, loginUser.getUsername());
        wrapper.orderByDesc(OaApprovalDelegation::getCreateTime);
        return delegationManager.list(wrapper);
    }

    /**
     * 查找某审批人当前生效的委托
     *
     * @param approverUsername 审批人账号
     * @param businessType    业务类型
     * @param tenantId        租户ID
     * @return 生效的委托记录，未找到返回null
     */
    public OaApprovalDelegation findActiveDelegation(String approverUsername, String businessType, Long tenantId) {
        LambdaQueryWrapper<OaApprovalDelegation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaApprovalDelegation::getTenantId, tenantId);
        wrapper.eq(OaApprovalDelegation::getDelegator, approverUsername);
        wrapper.eq(OaApprovalDelegation::getEnabled, 1);
        wrapper.le(OaApprovalDelegation::getStartDate, new Date());
        wrapper.ge(OaApprovalDelegation::getEndDate, new Date());
        wrapper.and(w -> w.isNull(OaApprovalDelegation::getBusinessTypes)
                .or()
                .eq(OaApprovalDelegation::getBusinessTypes, "")
                .or()
                .like(OaApprovalDelegation::getBusinessTypes, businessType));
        wrapper.last("LIMIT 1");
        return delegationManager.getOne(wrapper);
    }

    /**
     * 解析日期字符串
     */
    private Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            throw new BusinessException("日期不能为空");
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            throw new BusinessException("日期格式错误，请使用yyyy-MM-dd格式");
        }
    }
}
