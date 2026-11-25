package com.qykj.finmaster.workorder.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.manager.UserManager;
import com.qykj.finmaster.workorder.domain.WorkOrderRoleConfig;
import com.qykj.finmaster.workorder.domain.req.SaveRoleConfigReq;
import com.qykj.finmaster.workorder.service.WorkOrderRoleConfigService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 任务单可选负责人白名单管理 Controller
 */
@RestController
@RequestMapping("/work-order/role-config")
public class WorkOrderRoleConfigController extends BaseController {

    @Autowired
    private WorkOrderRoleConfigService service;

    @Autowired
    private UserManager userManager;

    /**
     * 查配置（管理端回显）
     */
    @GetMapping("/list")
    public Result<List<WorkOrderRoleConfig>> list(
            @RequestParam("taskType") String taskType,
            @RequestParam(value = "stepCode", required = false) String stepCode) {
        return Result.ok(service.listConfig(taskType, stepCode, getTenantId()));
    }

    /**
     * 查某任务类型/节点下的允许用户 ID 列表（任务分派时选人用）
     */
    @GetMapping("/allowed-users")
    public Result<List<Long>> allowedUsers(
            @RequestParam("taskType") String taskType,
            @RequestParam(value = "stepCode", required = false) String stepCode) {
        return Result.ok(service.listAllowedUserIds(taskType, stepCode, getTenantId()));
    }

    /**
     * 直接返回某节点白名单内的用户对象（一次查询直出 username/realName，前端选人不再需要再调用户列表）
     * 白名单为空时返回空列表，由前端决定是否降级到全量用户。
     */
    @GetMapping("/allowed-user-list")
    public Result<List<User>> allowedUserList(
            @RequestParam("taskType") String taskType,
            @RequestParam(value = "stepCode", required = false) String stepCode) {
        List<Long> ids = service.listAllowedUserIds(taskType, stepCode, getTenantId());
        if (ids == null || ids.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }
        List<User> users = userManager.listByIds(ids);
        // 不返回密码字段
        users.forEach(u -> u.setPassword(null));
        return Result.ok(users);
    }

    /**
     * 保存白名单（覆盖式）
     */
    @PostMapping("/save")
    public Result<Void> save(@RequestBody @Valid SaveRoleConfigReq req) {
        service.saveConfig(req, getLoginUser(), getTenantId());
        return Result.ok();
    }
}
