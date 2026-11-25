package com.qykj.finmaster.security;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.system.role.mapper.RoleDataScopeMapper;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.entity.UserRole;
import com.qykj.finmaster.system.user.manager.impl.UserManagerImpl;
import com.qykj.finmaster.system.user.manager.impl.UserRoleManagerImpl;
import com.qykj.finmaster.system.user.service.UserServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashSet;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private RoleDataScopeMapper roleDataScopeMapper;

    @Autowired
    private UserManagerImpl userManager;

    @Autowired
    private UserRoleManagerImpl userRoleManager;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userManager.lambdaQuery()
                .eq(User::getUsername, username)
                .one();
        if (ObjectUtils.isEmpty(user)) {
            throw new BusinessException(ResultMsg.USER_NOT_USER);
        }

        // 获取用户权限
        List<String> permissions = userService.getUserPermissions(user.getId());
        //获取数据权限
        Long dataScope =roleDataScopeMapper.getDataScopeByUserId(user.getId());
        // 获取数据权限下的用户ID列表
        List<String> dataScopeUserIds = userService.getDataScopeUserIds(user.getUsername(),user.getId(),dataScope);
        List<Long> dataScopeUserIdList = userService.getDataScopeUserList(user.getUsername(),user.getId(),dataScope);
        List<Long> userRoleIds = getUserRoleIds(user.getId());
        // 创建LoginUser对象
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setRealName(user.getRealName());
        if(CollUtil.isNotEmpty(userRoleIds)) {
            loginUser.setRoleIds(userRoleIds);
            // 使用用户名判断是否为超级管理员
            loginUser.setSuperAdmin(userRoleIds.contains(1L));
            // 是否为管理员（包含超级管理员）
            loginUser.setAdmin(!Collections.disjoint(userRoleIds, GlobalConstant.allAdminRoleIds));
        }else{
            // 使用用户名判断是否为超级管理员
            loginUser.setSuperAdmin("admin".equals(username));
            loginUser.setAdmin(Boolean.FALSE);
        }
        loginUser.setPermissions(new HashSet<>(permissions));
        loginUser.setDataScopeUserIds(dataScopeUserIds);
        loginUser.setDataScopeUserList(dataScopeUserIdList);
        loginUser.setDataScope(dataScope);
        loginUser.setTenantId(user.getTenantId()); // 设置租户ID
        return loginUser;
    }

    private List<Long> getUserRoleIds(Long userId){
        List<UserRole> userRoleList = userRoleManager.lambdaQuery()
                .eq(UserRole::getUserId, userId)
                .list();
        if(CollUtil.isNotEmpty(userRoleList)){
            return userRoleList.stream().map(UserRole::getRoleId).distinct().toList();
        }else{
            return Collections.emptyList();
        }
    }
}
