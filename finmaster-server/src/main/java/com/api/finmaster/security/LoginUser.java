package com.qykj.finmaster.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登录用户信息
 */
@Data
public class LoginUser implements UserDetails {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String realName;
    
    /**
     * 密码
     */
    private String password;
    
    /**
     * 是否超级管理员和管理员（不包含部门管理员）
     */
    private boolean isAdmin;

    /**
     * 权限列表
     */
    private Set<String> permissions;

    /**
     * 数据权限下的用户ID列表
     */
    private List<String> dataScopeUserIds;

    private List<Long> dataScopeUserList;

    private Long dataScope;
    
    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 角色ID
     */
    private List<Long> roleIds;

    /**
     * 是否超级管理员
     */
    private boolean isSuperAdmin;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null) {
            return Collections.emptyList();
        }
        return permissions.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toList());
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
} 