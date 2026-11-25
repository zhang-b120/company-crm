package com.qykj.finmaster.system.user.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.system.department.entity.resp.DeptUserInfoResp;
import com.qykj.finmaster.system.user.entity.req.*;
import com.qykj.finmaster.system.user.service.UserServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/system/user")
public class UserController extends BaseController {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private PasswordEncoder passwordEncoder;

    /*
    * 获取姓名
    * */
    @GetMapping("/realName")
    public AjaxResult getRealName() {
        List<String> realNames = userService.getAllRealNames();
        return AjaxResult.success(realNames);
    }

    @PostMapping("/login")
    public AjaxResult login(@Valid @RequestBody LoginRequest request) {
        return AjaxResult.success(userService.login(request));
    }

    @PostMapping("/login-by-params")
    public AjaxResult loginByParams(@RequestParam String username, @RequestParam String password) {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);
        return AjaxResult.success(userService.login(loginRequest));
    }

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    public TableDataInfo list(GetUserPageReq req) {
        Long tenantId = getTenantId();
        return getDataTable(userService.list(req, tenantId));
    }

    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    public AjaxResult add(@RequestBody AddUserReq req) {
        Long tenantId = getTenantId();
        return AjaxResult.success(userService.addUser(req, tenantId, getLoginUser()));
    }

    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    public AjaxResult update(@RequestBody UpdateUserReq req) {
        return AjaxResult.success(userService.updateUser(req, getTenantId(), getLoginUser()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:user:delete')")
    public AjaxResult delete(@PathVariable Long id) {
        return AjaxResult.success(userService.removeUser(id));
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    public AjaxResult assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        return AjaxResult.success(userService.assignRoles(userId, roleIds, getTenantId(), getLoginUser()));
    }

    @GetMapping("/{userId}/roles")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    public AjaxResult getUserRoles(@PathVariable Long userId) {
        return AjaxResult.success(userService.getUserRoles(userId));
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    public AjaxResult resetPassword(@PathVariable Long userId) {
        return AjaxResult.success(userService.resetPassword(userId));
    }

    @PostMapping("/admin-update-password")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    public AjaxResult adminUpdatePassword(@RequestBody UpdatePasswordReq req) {
        return AjaxResult.success(userService.resetPassword(req.getId(), req.getPassword()));
    }

    @GetMapping("/getInfo")
    public AjaxResult getInfo() {
        return AjaxResult.success(userService.getInfo(getLoginUser()));
    }

    /**
     * 测试密码加密和验证（仅用于调试）
     */
    @PostMapping("/test-password")
    public Result<Map<String, Object>> testPassword(@RequestParam String password) {
        try {
            String encoded = passwordEncoder.encode(password);
            boolean matches = passwordEncoder.matches(password, encoded);

            Map<String, Object> result = new HashMap<>();
            result.put("originalPassword", password);
            result.put("encodedPassword", encoded);
            result.put("matches", matches);

            return Result.ok(result);
        } catch (Exception e) {
            logger.error("密码测试失败: {}", e.getMessage(), e);
            return Result.error("密码测试失败：" + e.getMessage());
        }
    }

    /**
     * 获取部门员工用户信息
     * @param departmentId
     * @return
     */
    @PostMapping("/getDepartmentUser")
    public AjaxResult getDepartmentUser(@RequestParam Long departmentId) {
        return AjaxResult.success(userService.getDepartmentUser(departmentId));
    }
    /**
     * 获取部门员工用户信息
     * @param roleId
     * @return
     */
    @PostMapping("/getRoleUser")
    public AjaxResult getRoleUser(@RequestParam Long roleId) {
        return AjaxResult.success(userService.getRoleUser(roleId));
    }

    /**
     * 根据部门及其所有子部门查询部门与员工信息
     * @param deptId 根部门ID
     * @return 部门 + 员工信息列表
     */
    @PostMapping("/deptUsersWithChildren")
    public AjaxResult deptUsersWithChildren(@RequestParam Long deptId) {
        return AjaxResult.success(userService.selectDeptUsersWithChildren(deptId));
    }
}
