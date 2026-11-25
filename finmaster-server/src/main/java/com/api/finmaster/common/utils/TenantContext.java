//package com.qykj.finmaster.common.utils;
//
//import com.qykj.finmaster.security.LoginUser;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//
///**
// * 租户上下文工具类
// * 用于存储和获取当前用户的租户ID
// */
//public class TenantContext {
//
//    private static final Logger logger = LoggerFactory.getLogger(TenantContext.class);
//
//    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
//    private static final ThreadLocal<Boolean> TENANT_DISABLED = new ThreadLocal<>();
//
//    /**
//     * 设置当前租户ID
//     * @param tenantId 租户ID
//     */
//    public static void setTenantId(Long tenantId) {
//        TENANT_ID.set(tenantId);
//        logger.debug("设置租户ID: {}", tenantId);
//    }
//
//    /**
//     * 获取当前租户ID
//     * @return 租户ID，如果未设置则返回null
//     */
//    public static Long getTenantId() {
//        Long tenantId = TENANT_ID.get();
//        logger.debug("获取租户ID: {}", tenantId);
//        return tenantId;
//    }
//
//    /**
//     * 从当前登录用户获取租户ID
//     * @return 租户ID，如果获取失败则返回默认租户ID 1
//     */
//    public static Long getCurrentTenantId() {
//        // 如果租户过滤被禁用，返回null
//        if (isTenantDisabled()) {
//            logger.debug("租户过滤已禁用，返回null");
//            return null;
//        }
//
//        try {
//            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//            if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
//                LoginUser loginUser = (LoginUser) authentication.getPrincipal();
//                return loginUser.getTenantId();
//            }
//            return null;
//        } catch (Exception e) {
//            logger.warn("获取当前用户租户ID失败", e);
//            return null;
//        }
//    }
//
//    /**
//     * 清除当前租户ID
//     */
//    public static void clear() {
//        TENANT_ID.remove();
//        TENANT_DISABLED.remove();
//        logger.debug("清除租户ID和禁用状态");
//    }
//
//    /**
//     * 检查是否为超级管理员（租户ID为1）
//     * @return 是否为超级管理员
//     */
//    public static boolean isSuperAdmin() {
//        Long tenantId = getCurrentTenantId();
//        return tenantId != null && tenantId == 1L;
//    }
//
//    /**
//     * 临时禁用租户过滤
//     */
//    public static void disableTenant() {
//        TENANT_DISABLED.set(true);
//        logger.debug("临时禁用租户过滤");
//    }
//
//    /**
//     * 启用租户过滤
//     */
//    public static void enableTenant() {
//        TENANT_DISABLED.set(false);
//        logger.debug("启用租户过滤");
//    }
//
//    /**
//     * 检查租户过滤是否被禁用
//     * @return 是否禁用
//     */
//    public static boolean isTenantDisabled() {
//        Boolean disabled = TENANT_DISABLED.get();
//        return disabled != null && disabled;
//    }
//
//    /**
//     * 使用临时禁用租户过滤执行操作
//     * @param runnable 要执行的操作
//     */
//    public static void withoutTenant(Runnable runnable) {
//        boolean originalState = isTenantDisabled();
//        try {
//            disableTenant();
//            runnable.run();
//        } finally {
//            if (!originalState) {
//                enableTenant();
//            }
//        }
//    }
//}