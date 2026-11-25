package com.qykj.finmaster.oa.leave.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.oa.approval.service.ApprovalServiceImpl;
import com.qykj.finmaster.oa.leave.domain.OaLeave;
import com.qykj.finmaster.oa.leave.domain.OaLeaveQuota;
import com.qykj.finmaster.oa.leave.domain.req.AddLeaveReq;
import com.qykj.finmaster.oa.leave.domain.req.GetLeaveListReq;
import com.qykj.finmaster.oa.leave.manager.impl.OaLeaveManagerImpl;
import com.qykj.finmaster.oa.leave.manager.impl.OaLeaveQuotaManagerImpl;
import com.qykj.finmaster.oa.overtime.service.OvertimeServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 请假服务实现类
 */
@Slf4j
@Service
public class LeaveServiceImpl {

    @Autowired
    private OaLeaveManagerImpl leaveManager;

    @Autowired
    private OaLeaveQuotaManagerImpl leaveQuotaManager;

    @Autowired
    private ApprovalServiceImpl approvalService;

    @Lazy
    @Autowired
    private OvertimeServiceImpl overtimeService;

    /**
     * 新增请假申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addLeave(AddLeaveReq req, Long tenantId, LoginUser loginUser) {
        OaLeave leave = new OaLeave();
        leave.setTenantId(tenantId);
        leave.setApplicant(loginUser.getUsername());
        leave.setApplicantName(loginUser.getRealName());
        leave.setLeaveType(req.getLeaveType());
        leave.setStartTime(req.getStartTime());
        leave.setEndTime(req.getEndTime());
        leave.setDuration(req.getDuration());
        leave.setReason(req.getReason());
        leave.setHalfDay(req.getHalfDay() != null ? req.getHalfDay() : 0);
        leave.setHalfDayPeriod(req.getHalfDayPeriod());
        leave.setOvertimeRecordId(req.getOvertimeRecordId());
        leave.setStatus("DRAFT");
        leave.setCreateBy(loginUser.getUsername());
        leave.setCreateByName(loginUser.getRealName());
        leave.setCreateTime(new Date());
        leaveManager.save(leave);

        log.info("新增请假申请成功，ID: {}，申请人: {}", leave.getId(), loginUser.getUsername());
        return leave.getId();
    }

    /**
     * 提交请假申请（变为PENDING状态，并发起审批流程）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitLeave(Long id, Long tenantId, LoginUser loginUser) {
        OaLeave leave = leaveManager.getById(id);
        if (leave == null) {
            throw new BusinessException("请假申请不存在");
        }
        if (!"DRAFT".equals(leave.getStatus())) {
            throw new BusinessException("只有草稿状态的申请才能提交");
        }

        // 校验假期余额（调休类型除外，调休以加班记录为准）
        if (!"调休".equals(leave.getLeaveType()) && !"COMP".equalsIgnoreCase(leave.getLeaveType())) {
            checkQuota(tenantId, loginUser.getUserId(), loginUser.getUsername(),
                    leave.getLeaveType(), leave.getDuration());
        }

        // 更新状态为PENDING
        leave.setStatus("PENDING");
        leave.setUpdateBy(loginUser.getUsername());
        leave.setUpdateByName(loginUser.getRealName());
        leave.setUpdateTime(new Date());

        // 提交审批
        String title = loginUser.getRealName() + "的请假申请（" + leave.getLeaveType() + "）";
        Long instanceId = approvalService.submit("LEAVE", leave.getId(), title, tenantId, loginUser);

        leave.setApprovalInstanceId(instanceId);
        leaveManager.updateById(leave);

        log.info("请假申请提交审批成功，请假ID: {}，审批实例ID: {}", id, instanceId);
        return instanceId;
    }

    /**
     * 更新请假状态（由审批引擎回调）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLeaveStatus(Long leaveId, String status) {
        OaLeave leave = leaveManager.getById(leaveId);
        if (leave == null) {
            throw new BusinessException("请假申请不存在");
        }
        leave.setStatus(status);
        leave.setUpdateTime(new Date());
        leaveManager.updateById(leave);

        // 审批通过：扣减配额
        if ("APPROVED".equals(status)) {
            try {
                deductQuota(leave.getTenantId(), leave.getApplicant(), leave.getLeaveType(), leave.getDuration());
                // 调休：标记加班记录为已使用
                if (leave.getOvertimeRecordId() != null) {
                    overtimeService.markAsUsed(leave.getOvertimeRecordId());
                }
            } catch (Exception e) {
                log.error("假期配额扣减失败，leaveId: {}", leaveId, e);
            }
        }

        // 驳回或撤回：归还配额（如果之前已扣）
        if ("REJECTED".equals(status) || "DRAFT".equals(status)) {
            try {
                if (leave.getOvertimeRecordId() != null) {
                    overtimeService.markAsUnused(leave.getOvertimeRecordId());
                }
            } catch (Exception e) {
                log.error("加班记录状态恢复失败，leaveId: {}", leaveId, e);
            }
        }

        log.info("请假申请状态更新，ID: {}，新状态: {}", leaveId, status);
    }

    /**
     * 查询请假列表
     */
    public List<OaLeave> getList(GetLeaveListReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaLeave> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaLeave::getTenantId, tenantId);

        if (!loginUser.isAdmin()) {
            wrapper.eq(OaLeave::getApplicant, loginUser.getUsername());
        } else if (StringUtils.isNotEmpty(req.getApplicant())) {
            wrapper.eq(OaLeave::getApplicant, req.getApplicant());
        }

        if (StringUtils.isNotEmpty(req.getStatus())) {
            wrapper.eq(OaLeave::getStatus, req.getStatus());
        }
        if (StringUtils.isNotEmpty(req.getLeaveType())) {
            wrapper.eq(OaLeave::getLeaveType, req.getLeaveType());
        }

        wrapper.orderByDesc(OaLeave::getCreateTime);
        return leaveManager.list(wrapper);
    }

    /**
     * 获取请假详情
     */
    public OaLeave getDetail(Long id) {
        OaLeave leave = leaveManager.getById(id);
        if (leave == null) {
            throw new BusinessException("请假申请不存在");
        }
        return leave;
    }

    /**
     * 查询我的当年假期余额
     */
    public List<OaLeaveQuota> getMyQuota(Long tenantId, Long userId) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        LambdaQueryWrapper<OaLeaveQuota> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaLeaveQuota::getTenantId, tenantId);
        wrapper.eq(OaLeaveQuota::getUserId, userId);
        wrapper.eq(OaLeaveQuota::getYear, year);
        return leaveQuotaManager.list(wrapper);
    }

    /**
     * 查询所有员工假期配额（管理员）
     */
    public List<OaLeaveQuota> getAllQuota(Long tenantId, Integer year) {
        LambdaQueryWrapper<OaLeaveQuota> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaLeaveQuota::getTenantId, tenantId);
        if (year != null) {
            wrapper.eq(OaLeaveQuota::getYear, year);
        }
        wrapper.orderByDesc(OaLeaveQuota::getYear);
        return leaveQuotaManager.list(wrapper);
    }

    /**
     * 设置假期配额（新增或更新）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveQuota(OaLeaveQuota quota) {
        LambdaQueryWrapper<OaLeaveQuota> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaLeaveQuota::getTenantId, quota.getTenantId());
        wrapper.eq(OaLeaveQuota::getUserId, quota.getUserId());
        wrapper.eq(OaLeaveQuota::getLeaveType, quota.getLeaveType());
        wrapper.eq(OaLeaveQuota::getYear, quota.getYear());
        OaLeaveQuota existing = leaveQuotaManager.getOne(wrapper);
        if (existing != null) {
            existing.setTotalDays(quota.getTotalDays());
            existing.setUpdateTime(new Date());
            leaveQuotaManager.updateById(existing);
        } else {
            quota.setUsedDays(BigDecimal.ZERO);
            quota.setCreateTime(new Date());
            quota.setUpdateTime(new Date());
            leaveQuotaManager.save(quota);
        }
    }

    /**
     * 校验假期余额是否充足
     */
    private void checkQuota(Long tenantId, Long userId, String username, String leaveType, BigDecimal duration) {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        LambdaQueryWrapper<OaLeaveQuota> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaLeaveQuota::getTenantId, tenantId);
        wrapper.eq(OaLeaveQuota::getUserId, userId);
        wrapper.eq(OaLeaveQuota::getLeaveType, leaveType);
        wrapper.eq(OaLeaveQuota::getYear, year);
        OaLeaveQuota quota = leaveQuotaManager.getOne(wrapper);

        if (quota == null) {
            // 无配额记录，允许申请（管理员可能未配置）
            log.warn("未找到假期配额，tenantId: {}, userId: {}, leaveType: {}", tenantId, userId, leaveType);
            return;
        }

        BigDecimal remaining = quota.getTotalDays().subtract(quota.getUsedDays());
        if (duration != null && duration.compareTo(remaining) > 0) {
            throw new BusinessException(
                String.format("假期余额不足，%s剩余%.1f天，申请%.1f天",
                    leaveType, remaining.doubleValue(), duration.doubleValue()));
        }
    }

    /**
     * 扣减假期配额
     */
    private void deductQuota(Long tenantId, String username, String leaveType, BigDecimal duration) {
        if (duration == null || duration.compareTo(BigDecimal.ZERO) <= 0) return;

        int year = Calendar.getInstance().get(Calendar.YEAR);
        LambdaQueryWrapper<OaLeaveQuota> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaLeaveQuota::getTenantId, tenantId);
        wrapper.eq(OaLeaveQuota::getUsername, username);
        wrapper.eq(OaLeaveQuota::getLeaveType, leaveType);
        wrapper.eq(OaLeaveQuota::getYear, year);
        OaLeaveQuota quota = leaveQuotaManager.getOne(wrapper);

        if (quota != null) {
            quota.setUsedDays(quota.getUsedDays().add(duration));
            quota.setUpdateTime(new Date());
            leaveQuotaManager.updateById(quota);
            log.info("假期配额扣减，username: {}, leaveType: {}, 扣减: {}天", username, leaveType, duration);
        }
    }
}
