package com.qykj.finmaster.system.user.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.department.entity.resp.DeptUserInfoResp;
import com.qykj.finmaster.system.department.entity.Department;
import com.qykj.finmaster.system.department.entity.resp.GetDepartmentResp;
import com.qykj.finmaster.system.department.service.DepartmentServiceImpl;
import com.qykj.finmaster.system.role.entity.Role;
import com.qykj.finmaster.system.role.mapper.RoleDataScopeMapper;
import com.qykj.finmaster.system.role.service.RoleServiceImpl;
import com.qykj.finmaster.system.user.entity.UserDepartment;
import com.qykj.finmaster.system.user.entity.UserRole;
import com.qykj.finmaster.system.user.entity.req.AddUserReq;
import com.qykj.finmaster.system.user.entity.req.GetUserPageReq;
import com.qykj.finmaster.system.user.entity.req.LoginRequest;
import com.qykj.finmaster.system.user.entity.req.UpdateUserReq;
import com.qykj.finmaster.system.user.entity.resp.GetUserInfoResp;
import com.qykj.finmaster.system.user.entity.resp.GetUserPageResp;
import com.qykj.finmaster.system.user.entity.resp.LoginResponse;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.manager.impl.UserDepartmentManagerImpl;
import com.qykj.finmaster.system.user.manager.impl.UserManagerImpl;
import com.qykj.finmaster.system.user.manager.impl.UserRoleManagerImpl;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;

import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RoleServiceImpl roleService;

    @Resource
    private RoleDataScopeMapper roleDataScopeMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private Long jwtExpiration;

    @Resource
    private DepartmentServiceImpl departmentService;
    @Autowired
    private UserManagerImpl userManager;
    @Autowired
    private UserDepartmentManagerImpl userDepartmentManager;
    @Autowired
    private UserRoleManagerImpl userRoleManager;


    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 登录
     * @param request
     * @return
     */
    public LoginResponse login(LoginRequest request) {
        User user = userManager.lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .one();

        if (ObjectUtils.isEmpty(user)) {
            throw new BusinessException(ResultMsg.USER_USERNAME_PASSWORD_ERROR);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultMsg.USER_USERNAME_PASSWORD_ERROR);
        }
        if (!user.getEnabled()) {
            throw new BusinessException(ResultMsg.USER_USERNAME_DISABLE);
        }

        String token = generateToken(user);
        return LoginResponse.builder()
                .token(token)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .realName(user.getRealName())
                        .email(user.getEmail())
                        .phone(user.getPhone())
                        .avatar(user.getAvatar())
                        .build())
                .build();
    }

    /**
     * 获取token
     * @param user
     * @return
     */
    private String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 获取所有用户名
     * @return
     */
    public List<String> getAllRealNames() {
        return userMapper.selectAllRealNames();
    }

    /**
     * 根据部门及其所有子部门查询部门与员工信息
     * @param deptId 部门ID（根部门）
     * @return 部门 + 员工信息列表
     */
    public List<DeptUserInfoResp> selectDeptUsersWithChildren(Long deptId) {
        if (deptId == null) {
            throw new BusinessException(ResultMsg.PARAM_IS_NULL);
        }
        return userMapper.selectDeptUsersWithChildren(deptId);
    }

    /**
     * 分配角色
     *
     * @param userId
     * @param roleIds
     * @param tenantId
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignRoles(Long userId, List<Long> roleIds, Long tenantId, LoginUser loginUser) {
        // 先删除原有角色
        userRoleManager.lambdaUpdate()
                .set(UserRole::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), UserRole::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), UserRole::getUpdateByName, loginUser.getRealName())
                .set(UserRole::getUpdateTime, new Date())
                .eq(UserRole::getUserId, userId)
                .update();
        // 添加新角色
        if (CollUtil.isNotEmpty(roleIds)) {
            List<UserRole> userRoleList = roleIds.stream().map(item -> {
                UserRole userRole = new UserRole();
                userRole.setUserId(userId);
                userRole.setRoleId(item);
                userRole.setTenantId(tenantId);
                if (ObjectUtils.isNotEmpty(loginUser)) {
                    userRole.setUpdateBy(loginUser.getUsername());
                    userRole.setCreateByName(loginUser.getRealName());
                }
                userRole.setCreateTime(new Date());
                return userRole;
            }).toList();
            userRoleManager.saveBatch(userRoleList);
        }
        return Boolean.TRUE;
    }

    /**
     * 分配部门
     * @param userId
     * @param departmentIds
     * @param tenantId
     * @param loginUser
     */
    @Transactional(rollbackFor = Exception.class)
    public void assignDepartments(Long userId, List<Long> departmentIds, Long tenantId, LoginUser loginUser) {
        userDepartmentManager.lambdaUpdate()
                .set(UserDepartment::getDeleted, GlobalConstant.Number.ONE_INT)
                .eq(UserDepartment::getUserId, userId)
                .update();
        if (departmentIds != null && !departmentIds.isEmpty()) {
            List<UserDepartment> userDepartmentList = departmentIds.stream().map(item -> {
                UserDepartment userDepartment = new UserDepartment();
                userDepartment.setUserId(userId);
                userDepartment.setDepartmentId(item);
                userDepartment.setTenantId(tenantId);
                userDepartment.setCreateTime(new Date());
                if (ObjectUtils.isNotEmpty(loginUser)) {
                    userDepartment.setCreateBy(loginUser.getUsername());
                    userDepartment.setCreateByName(loginUser.getRealName());
                }
                return userDepartment;
            }).toList();
            userDepartmentManager.saveBatch(userDepartmentList);
        }
    }

    /**
     * 重置密码
     * @param userId
     * @return
     */
    public Boolean resetPassword(Long userId) {
        userManager.lambdaUpdate()
                .set(User::getPassword, passwordEncoder.encode(GlobalConstant.DEFAULT_USER_PASSWORD))
                .eq(User::getId, userId)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 重置密码
     * @param id
     * @param password
     * @return
     */
    public Boolean resetPassword(Long id, String password) {
        userManager.lambdaUpdate()
                .set(User::getPassword, passwordEncoder.encode(password))
                .eq(User::getId, id)
                .update();
        return Boolean.TRUE;
    }

    public void updateUser(User user) {
        // 如果提供了密码，需要加密
        if (StringUtils.hasText(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setUpdateTime(new Date());
        userMapper.updateById(user);
    }

    /**
     * 查询用户列表
     * @param req
     * @param tenantId
     * @return
     */
    public List<GetUserPageResp> list(GetUserPageReq req, Long tenantId) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        params.put("tenantId", tenantId);
        List<GetUserPageResp> list = userMapper.selectUserList(params);
        if(ObjectUtils.isEmpty(list)){
            return Collections.emptyList();
        }
        List<Long> idList = list.stream().map(GetUserPageResp::getId).toList();
        List<UserDepartment> userDepartmentList = userDepartmentManager.lambdaQuery()
                .in(UserDepartment::getUserId, idList)
                .list();
        if(CollUtil.isNotEmpty(userDepartmentList)){
            Map<Long, List<Long>> userDepartmentMap = userDepartmentList.stream()
                    .collect(Collectors.groupingBy(UserDepartment::getUserId, Collectors.mapping(UserDepartment::getDepartmentId, Collectors.toList())));
            list.forEach(item -> {
                if(CollUtil.isNotEmpty(userDepartmentMap) && CollUtil.isNotEmpty(userDepartmentMap.get(item.getId()))){
                    item.setDepartmentIds(userDepartmentMap.get(item.getId()));
                }
            });
        }
        return list;
    }

    /**
     * 新增用户
     * @param req
     * @param tenantId
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addUser(AddUserReq req, Long tenantId, LoginUser loginUser) {
        User user = CopierCopyUtils.convertTo(req, User.class);
        if (checkUserNameUnique(user.getUsername())) {
            throw new BusinessException(MessageFormat.format(ResultMsg.USER_ADD_EXISTS_USERNAME, user.getUsername()));
        }
        user.setTenantId(tenantId);
        user.setPassword(passwordEncoder.encode(GlobalConstant.DEFAULT_USER_PASSWORD));
        user.setCreateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            user.setCreateBy(loginUser.getUsername());
            user.setCreateByName(loginUser.getRealName());
        }
        userManager.save(user);
        this.assignDepartments(user.getId(),req.getDepartmentIds(),tenantId,loginUser);
        return Boolean.TRUE;
    }

    /**
     * 修改用户
     * @param req
     * @param tenantId
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUser(UpdateUserReq req, Long tenantId, LoginUser loginUser) {
        User user = CopierCopyUtils.convertTo(req, User.class);
        user.setTenantId(tenantId);
        user.setUpdateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            user.setUpdateBy(loginUser.getUsername());
            user.setUpdateByName(loginUser.getRealName());
        }
        userManager.updateById(user);
        this.assignDepartments(user.getId(),req.getDepartmentIds(),tenantId, loginUser);
        return Boolean.TRUE;
    }

    /**
     * 删除用户
     * @param id
     * @return
     */
    public Boolean removeUser(Long id) {
        userManager.getBaseMapper().deleteById(id);
        return Boolean.TRUE;
    }

    /**
     * 获取用户角色
     * @param userId
     */
    public List<Role> getUserRoles(Long userId) {
        List<Role> roleList = roleService.findRolesByUserId(userId);
        if(CollUtil.isEmpty(roleList)){
            return Collections.emptyList();
        }
        return roleList;
    }

    /**
     * 获取当前登录用户信息
     * @param loginUser
     */
    public Map<String, Object> getInfo(LoginUser loginUser) {
        // 1. 获取当前登录用户名
        if(ObjectUtils.isEmpty(loginUser)){
            return Collections.emptyMap();
        }
        String username = loginUser.getUsername();
        // 2. 查询用户信息
        User user = userManager.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
        if (ObjectUtils.isEmpty(user)) {
            throw new BusinessException(ResultMsg.USER_NOT_USER);
        }
        // 3. 查询角色
        List<Role> roleList = roleService.findRolesByUserId(user.getId());
        List<String> roles = roleList.stream().map(Role::getCode).toList();

        // 4. 查询权限
        List<String> permissions = this.getUserPermissions(user.getId());

        // 5. 查询用户部门
        List<UserDepartment> userDepartmentList = userDepartmentManager.lambdaQuery()
                .eq(UserDepartment::getUserId, user.getId())
                .eq(UserDepartment::getDeleted, GlobalConstant.Number.ZERO_INT)
                .list();
        List<Long> departmentIds = userDepartmentList.stream()
                .map(UserDepartment::getDepartmentId)
                .toList();

        // 6. 组装返回
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userName", user.getUsername());
        userMap.put("realName", user.getRealName());
        userMap.put("avatar", user.getAvatar());
        userMap.put("roles", roles);
        userMap.put("permissions", permissions);
        userMap.put("superAdmin", loginUser.isSuperAdmin());
        userMap.put("admin", loginUser.isAdmin());
        userMap.put("departmentIds", departmentIds);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "操作成功");
        result.put("user", userMap);
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean createTenantAdmin(String userName, String password, String realName,String email,
                                     String phone, Long tenantID, String tenantCode, LoginUser loginUser) {
        // 1. 生成或使用指定的管理员信息
        if (!StringUtils.hasText(userName)) {
            userName = "admin_" + tenantCode.toLowerCase();
        }
        // 2. 检查用户名是否已存在
        if (checkUserNameUnique(userName)) {
            throw new BusinessException(MessageFormat.format(ResultMsg.TENANT_ADMIN_EXISTS_USERNAME, userName));
        }
        // 3. 生成密码
        if (!StringUtils.hasText(password)) {
            password = GlobalConstant.DEFAULT_USER_PASSWORD;
        }
        // 4. 创建用户
        User adminUser = new User();
        adminUser.setUsername(userName);
        adminUser.setPassword(passwordEncoder.encode(password));
        adminUser.setRealName(realName);
        adminUser.setEmail(email);
        adminUser.setPhone(phone);
        adminUser.setEnabled(Boolean.TRUE);
        adminUser.setTenantId(tenantID);
        if(ObjectUtils.isEmpty(loginUser)){
           adminUser.setCreateBy(loginUser.getUsername());
           adminUser.setCreateByName(loginUser.getRealName());
        }
        adminUser.setCreateTime(new Date());
        userManager.save(adminUser);

        // 5. 分配系统超级管理员角色（ID=1）
        assignRoles(adminUser.getId(), Arrays.asList(GlobalConstant.Number.ONE_LONG), tenantID, loginUser);
        return Boolean.TRUE;
    }

    /**
     * 校验用户名唯一
     * @param userName
     * @return
     */
    public Boolean checkUserNameUnique(String userName){
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getUsername, userName);
        return userManager.getBaseMapper().exists(wrapper);
    }

    /**
     * 获取权限
     * @param userId
     * @return
     */
    public List<String> getUserPermissions(Long userId) {
        List<String> permissions;
        // 如果是admin用户，返回所有权限
        if (userId == 1L) {
            permissions = userMapper.getAllPermissions();
        } else {
            // 否则返回用户有权限的菜单和按钮
            permissions = userMapper.getUserPermissions(userId);
        }
        return permissions;
    }

    /**
     * 获取权限所属userID
     * @param userName
     * @param userId
     * @param dataScope
     * @return
     */
    public List<String> getDataScopeUserIds(String userName, Long userId, Long dataScope) {
        // 1. 全部数据权限，返回null表示不过滤
        if (dataScope == 1) {
            return null;
        }

        List<Long> deptIds = new ArrayList<>();
        List<Long> allDeptIds = new ArrayList<>();

        if (dataScope == 2) {
            // 2. 自定义数据权限
            String deptIdStr = roleDataScopeMapper.getDataScopeDeptsByUserId(userId);
            if (deptIdStr != null && !deptIdStr.isEmpty()) {
                String[] deptIdArr = deptIdStr.split(",");
                for (String idStr : deptIdArr) {
                    if (!idStr.trim().isEmpty()) {
                        deptIds.add(Long.parseLong(idStr.trim()));
                    }
                }
            }
            List<Department> allDepts = departmentService.list();
            List<GetDepartmentResp> deptTree = departmentService.buildTree(allDepts);
            allDeptIds.addAll(deptIds);
            for (Long id : deptIds) {
                getAllChildDeptIds(id, deptTree, allDeptIds);
            }
            return userMapper.getDataScopeUserIds(allDeptIds.toArray(new Long[0]));
        } else if (dataScope == 3 || dataScope == 4) {
            // 3. 本部门数据权限 或 4. 本部门及以下数据权限
            List<Long> userDeptIds =  new ArrayList<>();
            List<UserDepartment> userDepartmentList = userDepartmentManager.lambdaQuery()
                    .select(UserDepartment::getDepartmentId)
                    .eq(UserDepartment::getUserId, userId)
                    .list();
            if(CollUtil.isNotEmpty(userDepartmentList)){
                userDeptIds = userDepartmentList.stream()
                        .map(UserDepartment::getDepartmentId)
                        .toList();
            }
            if (userDeptIds != null && !userDeptIds.isEmpty()) {
                deptIds.addAll(userDeptIds);
            }
            List<Department> allDepts = departmentService.list();
            List<GetDepartmentResp> deptTree = departmentService.buildTree(allDepts);
            allDeptIds.addAll(deptIds);
            for (Long id : deptIds) {
                getAllChildDeptIds(id, deptTree, allDeptIds);
            }
            return userMapper.getDataScopeUserIds(allDeptIds.toArray(new Long[0]));
        } else if (dataScope == 5) {
            // 5. 仅本人数据权限
            return Collections.singletonList(userName);
        }
        return null;
    }


    /**
     * 获取权限所属userID
     * @param userName
     * @param userId
     * @param dataScope
     * @return
     */
    public List<Long> getDataScopeUserList(String userName, Long userId, Long dataScope) {
        // 1. 全部数据权限，返回null表示不过滤
        if (dataScope == 1) {
            return null;
        }

        List<Long> deptIds = new ArrayList<>();
        List<Long> allDeptIds = new ArrayList<>();

        if (dataScope == 2) {
            // 2. 自定义数据权限
            String deptIdStr = roleDataScopeMapper.getDataScopeDeptsByUserId(userId);
            if (deptIdStr != null && !deptIdStr.isEmpty()) {
                String[] deptIdArr = deptIdStr.split(",");
                for (String idStr : deptIdArr) {
                    if (!idStr.trim().isEmpty()) {
                        deptIds.add(Long.parseLong(idStr.trim()));
                    }
                }
            }
            List<Department> allDepts = departmentService.list();
            List<GetDepartmentResp> deptTree = departmentService.buildTree(allDepts);
            allDeptIds.addAll(deptIds);
            for (Long id : deptIds) {
                getAllChildDeptIds(id, deptTree, allDeptIds);
            }
            return userMapper.getDataScopeUserList(allDeptIds.toArray(new Long[0]));
        } else if (dataScope == 3 || dataScope == 4) {
            // 3. 本部门数据权限 或 4. 本部门及以下数据权限
            List<Long> userDeptIds =  new ArrayList<>();
            List<UserDepartment> userDepartmentList = userDepartmentManager.lambdaQuery()
                    .select(UserDepartment::getDepartmentId)
                    .eq(UserDepartment::getUserId, userId)
                    .list();
            if(CollUtil.isNotEmpty(userDepartmentList)){
                userDeptIds = userDepartmentList.stream()
                        .map(UserDepartment::getDepartmentId)
                        .toList();
            }
            if (userDeptIds != null && !userDeptIds.isEmpty()) {
                deptIds.addAll(userDeptIds);
            }
            List<Department> allDepts = departmentService.list();
            List<GetDepartmentResp> deptTree = departmentService.buildTree(allDepts);
            allDeptIds.addAll(deptIds);
            for (Long id : deptIds) {
                getAllChildDeptIds(id, deptTree, allDeptIds);
            }
            return userMapper.getDataScopeUserList(allDeptIds.toArray(new Long[0]));
        } else if (dataScope == 5) {
            // 5. 仅本人数据权限
            return Collections.singletonList(userId);
        }
        return null;
    }

    /**
     * 递归获取所有子部门id
     */
    private void getAllChildDeptIds(Long parentId, List<GetDepartmentResp> deptTree, List<Long> result) {
        for (GetDepartmentResp dept : deptTree) {
            if (dept.getId().equals(parentId) && dept.getChildren() != null) {
                for (GetDepartmentResp child : dept.getChildren()) {
                    if (!result.contains(child.getId())) {
                        result.add(child.getId());
                        getAllChildDeptIds(child.getId(), dept.getChildren(), result);
                    }
                }
            } else if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                getAllChildDeptIds(parentId, dept.getChildren(), result);
            }
        }
    }

    /**
     * 判断当前登录用户是否为管理员
     * @param loginUser
     * @return
     */
    public Boolean checkLoginUserIdAdmin(LoginUser loginUser){
        if(ObjectUtils.isEmpty(loginUser)){
            return Boolean.FALSE;
        }
        List<UserRole> userRoleList = userRoleManager.lambdaQuery()
                .eq(UserRole::getUserId, loginUser.getUserId())
                .list();
        if(ObjectUtils.isNotEmpty(userRoleList)) {
            List<Long> roleList = userRoleList.stream().map(UserRole::getRoleId).toList();
            if (roleList.contains(GlobalConstant.Number.ONE_LONG) || roleList.contains(GlobalConstant.Number.THREE_LONG)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * 获取角色相关员工用户信息
     * @param roleId
     * @return
     */
    public List<GetUserInfoResp> getRoleUser(Long roleId) {
        List<User> userList = userMapper.getRoleUser(roleId);
        if(CollUtil.isEmpty(userList)){
            return Collections.emptyList();
        }
        return CopierCopyUtils.convertTo(userList, GetUserInfoResp.class);
    }
    /**
     * 获取部门相关员工用户信息
     * @param departmentId
     * @return
     */
    public List<GetUserInfoResp> getDepartmentUser(Long departmentId) {
        List<User> userList = userMapper.getDepartmentUser(departmentId);
        if(CollUtil.isEmpty(userList)){
            return Collections.emptyList();
        }
        return CopierCopyUtils.convertTo(userList, GetUserInfoResp.class);
    }
}
