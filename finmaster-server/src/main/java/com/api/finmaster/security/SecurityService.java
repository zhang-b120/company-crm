package com.qykj.finmaster.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 权限校验服务
 */
@Component("ss")
public class SecurityService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
    
    /**
     * 判断当前用户是否具有指定权限
     *
     * @param permission 权限字符串
     * @return 是否具有权限
     */
    public boolean hasPermi(String permission) {
        if (permission == null || permission.isEmpty()) {
            logger.warn("权限字符串为空");
            return false;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            logger.warn("当前用户未认证");
            return false;
        }
        
        // 获取当前用户权限列表
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) principal;
            // 如果是超级管理员，直接返回true
            if (loginUser.isSuperAdmin()) {
                logger.debug("用户[{}]是超级管理员，拥有所有权限", loginUser.getUsername());
                return true;
            }
            // 判断是否包含指定权限
            boolean hasPermission = loginUser.getPermissions() != null && loginUser.getPermissions().contains(permission);
            logger.debug("用户[{}]权限检查: {} -> {}, 权限列表: {}", 
                loginUser.getUsername(), 
                permission, 
                hasPermission,
                loginUser.getPermissions());
            return hasPermission;
        }
        
        logger.warn("当前用户不是LoginUser类型: {}", principal.getClass().getName());
        return false;
    }
} 