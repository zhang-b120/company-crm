package com.qykj.finmaster.oa.overtime.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.oa.overtime.domain.OaOvertimeRecord;
import com.qykj.finmaster.oa.overtime.manager.impl.OaOvertimeRecordManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 加班记录服务
 */
@Slf4j
@Service
public class OvertimeServiceImpl {

    @Autowired
    private OaOvertimeRecordManagerImpl overtimeManager;

    /**
     * 添加加班记录
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addOvertime(OaOvertimeRecord req, Long tenantId, LoginUser loginUser) {
        OaOvertimeRecord record = new OaOvertimeRecord();
        record.setTenantId(tenantId);
        record.setUserId(loginUser.getUserId());
        record.setUsername(loginUser.getUsername());
        record.setRealName(loginUser.getRealName());
        record.setOvertimeDate(req.getOvertimeDate());
        record.setOvertimeHours(req.getOvertimeHours());
        // 每8小时折算1天调休
        BigDecimal compDays = req.getOvertimeHours().divide(BigDecimal.valueOf(8), 1, RoundingMode.HALF_UP);
        record.setCompDays(compDays);
        record.setStatus("UNUSED");
        record.setRemark(req.getRemark());
        record.setCreateBy(loginUser.getUsername());
        record.setCreateByName(loginUser.getRealName());
        record.setCreateTime(new Date());
        overtimeManager.save(record);
        return record.getId();
    }

    /**
     * 查询加班记录列表（当前用户）
     */
    public List<OaOvertimeRecord> getMyList(Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaOvertimeRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaOvertimeRecord::getTenantId, tenantId);
        if (!loginUser.isAdmin()) {
            wrapper.eq(OaOvertimeRecord::getUsername, loginUser.getUsername());
        }
        wrapper.orderByDesc(OaOvertimeRecord::getOvertimeDate);
        return overtimeManager.list(wrapper);
    }

    /**
     * 查询可用加班记录（未使用的，用于调休申请时选择）
     */
    public List<OaOvertimeRecord> getAvailableList(Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaOvertimeRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaOvertimeRecord::getTenantId, tenantId);
        wrapper.eq(OaOvertimeRecord::getUsername, loginUser.getUsername());
        wrapper.eq(OaOvertimeRecord::getStatus, "UNUSED");
        wrapper.orderByDesc(OaOvertimeRecord::getOvertimeDate);
        return overtimeManager.list(wrapper);
    }

    /**
     * 标记加班记录为已使用（审批通过调休时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsUsed(Long overtimeRecordId) {
        OaOvertimeRecord record = overtimeManager.getById(overtimeRecordId);
        if (record == null) {
            throw new BusinessException("加班记录不存在");
        }
        record.setStatus("USED");
        record.setUpdateTime(new Date());
        overtimeManager.updateById(record);
    }

    /**
     * 标记加班记录恢复为未使用（审批撤回或驳回时调用）
     */
    @Transactional(rollbackFor = Exception.class)
    public void markAsUnused(Long overtimeRecordId) {
        OaOvertimeRecord record = overtimeManager.getById(overtimeRecordId);
        if (record == null) return;
        record.setStatus("UNUSED");
        record.setUpdateTime(new Date());
        overtimeManager.updateById(record);
    }
}
