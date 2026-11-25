package com.qykj.finmaster.workorder.service;

import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.workorder.domain.WorkOrderRoleConfig;
import com.qykj.finmaster.workorder.domain.req.SaveRoleConfigReq;
import com.qykj.finmaster.workorder.manager.WorkOrderRoleConfigManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务单可选负责人白名单 Service
 * 两级配置：role_id（圈人范围）+ user_id（最终白名单）
 */
@Service
public class WorkOrderRoleConfigService {

    @Autowired
    private WorkOrderRoleConfigManager manager;

    /**
     * 查询某个任务类型 + 节点下的可选用户 ID 列表
     * stepCode 为 null 时返回 task_type 级全局配置
     */
    public List<Long> listAllowedUserIds(String taskType, String stepCode, Long tenantId) {
        if (StringUtils.isBlank(taskType)) return Collections.emptyList();
        List<WorkOrderRoleConfig> list = manager.lambdaQuery()
                .eq(WorkOrderRoleConfig::getTenantId, tenantId)
                .eq(WorkOrderRoleConfig::getTaskType, taskType)
                .and(wrapper -> {
                    if (stepCode == null) {
                        wrapper.isNull(WorkOrderRoleConfig::getStepCode);
                    } else {
                        wrapper.eq(WorkOrderRoleConfig::getStepCode, stepCode)
                                .or().isNull(WorkOrderRoleConfig::getStepCode);
                    }
                })
                .eq(WorkOrderRoleConfig::getEnabled, 1)
                .list();
        return list.stream().map(WorkOrderRoleConfig::getUserId).distinct().collect(Collectors.toList());
    }

    /** 查询某个任务类型 + 节点的完整配置（给管理端回显） */
    public List<WorkOrderRoleConfig> listConfig(String taskType, String stepCode, Long tenantId) {
        if (StringUtils.isBlank(taskType)) return Collections.emptyList();
        return manager.lambdaQuery()
                .eq(WorkOrderRoleConfig::getTenantId, tenantId)
                .eq(WorkOrderRoleConfig::getTaskType, taskType)
                .and(w -> {
                    if (stepCode == null) w.isNull(WorkOrderRoleConfig::getStepCode);
                    else w.eq(WorkOrderRoleConfig::getStepCode, stepCode);
                })
                .list();
    }

    /**
     * 保存白名单（以"覆盖"方式：先删除同 task_type + step_code 的记录，再批量插入）
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(SaveRoleConfigReq req, LoginUser loginUser, Long tenantId) {
        // 1) 清空旧配置
        manager.lambdaUpdate()
                .eq(WorkOrderRoleConfig::getTenantId, tenantId)
                .eq(WorkOrderRoleConfig::getTaskType, req.getTaskType())
                .and(w -> {
                    if (req.getStepCode() == null) w.isNull(WorkOrderRoleConfig::getStepCode);
                    else w.eq(WorkOrderRoleConfig::getStepCode, req.getStepCode());
                })
                .remove();

        // 2) 插入新配置
        if (req.getUserIds() == null || req.getUserIds().isEmpty()) return;
        Date now = new Date();
        for (Long userId : req.getUserIds()) {
            WorkOrderRoleConfig c = new WorkOrderRoleConfig();
            c.setTenantId(tenantId);
            c.setTaskType(req.getTaskType());
            c.setStepCode(req.getStepCode());
            c.setRoleId(req.getRoleId());
            c.setUserId(userId);
            c.setEnabled(1);
            c.setCreateBy(loginUser.getUsername());
            c.setCreateTime(now);
            c.setUpdateBy(loginUser.getUsername());
            c.setUpdateTime(now);
            manager.save(c);
        }
    }
}
