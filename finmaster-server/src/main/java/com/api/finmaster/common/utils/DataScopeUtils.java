package com.qykj.finmaster.common.utils;

import com.qykj.finmaster.common.core.domain.model.LoginUser;
import com.qykj.finmaster.common.core.domain.model.SysUser;
import com.qykj.finmaster.common.core.domain.model.SysRole;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据权限工具类
 */
public class DataScopeUtils {
    
    /**
     * 数据权限类型枚举
     */
    public enum DataScopeEnum {
        ALL("1", "全部数据权限"),
        CUSTOM("2", "自定义数据权限"),
        DEPT("3", "本部门数据权限"),
        SELF("4", "仅本人数据权限");

        private final String code;
        private final String info;

        DataScopeEnum(String code, String info) {
            this.code = code;
            this.info = info;
        }

        public String getCode() {
            return code;
        }

        public String getInfo() {
            return info;
        }
    }

    /**
     * 数据权限配置类
     */
    public static class DataScopeConfig<T> {
        private final List<T> dataList;
        private final LoginUser loginUser;
        private final List<String> filterFields;
        private final Map<Long, List<Long>> userDeptMap;
        private final List<Long> customDeptIds;

        private DataScopeConfig(Builder<T> builder) {
            this.dataList = builder.dataList;
            this.loginUser = builder.loginUser;
            this.filterFields = builder.filterFields;
            this.userDeptMap = builder.userDeptMap;
            this.customDeptIds = builder.customDeptIds;
        }

        public static class Builder<T> {
            private List<T> dataList;
            private LoginUser loginUser;
            private List<String> filterFields;
            private Map<Long, List<Long>> userDeptMap;
            private List<Long> customDeptIds;

            public Builder<T> dataList(List<T> dataList) {
                this.dataList = dataList;
                return this;
            }

            public Builder<T> loginUser(LoginUser loginUser) {
                this.loginUser = loginUser;
                return this;
            }

            public Builder<T> filterFields(List<String> filterFields) {
                this.filterFields = filterFields;
                return this;
            }

            public Builder<T> userDeptMap(Map<Long, List<Long>> userDeptMap) {
                this.userDeptMap = userDeptMap;
                return this;
            }

            public Builder<T> customDeptIds(List<Long> customDeptIds) {
                this.customDeptIds = customDeptIds;
                return this;
            }

            public DataScopeConfig<T> build() {
                return new DataScopeConfig<>(this);
            }
        }
    }

    /**
     * 过滤数据列表
     *
     * @param config 数据权限配置
     * @return 过滤后的数据列表
     */
    public static <T> List<T> filterDataList(DataScopeConfig<T> config) {
        if (config.dataList == null || config.dataList.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取当前用户
        SysUser currentUser = config.loginUser.getUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }

        // 如果是管理员，返回所有数据
        if (currentUser.isAdmin()) {
            return config.dataList;
        }

        // 获取用户角色
        List<SysRole> roles = config.loginUser.getRoles();
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取最小的数据权限范围
        String dataScope = roles.stream()
                .map(SysRole::getDataScope)
                .min(String::compareTo)
                .orElse(DataScopeEnum.SELF.getCode());

        // 根据数据权限范围过滤数据
        switch (dataScope) {
            case "1": // 全部数据权限
                return config.dataList;
            case "2": // 自定义数据权限
                return filterByCustomDepts(config);
            case "3": // 本部门数据权限
                return filterByDept(config);
            case "4": // 仅本人数据权限
                return filterBySelf(config);
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 根据自定义部门过滤数据
     */
    private static <T> List<T> filterByCustomDepts(DataScopeConfig<T> config) {
        if (config.customDeptIds == null || config.customDeptIds.isEmpty()) {
            return Collections.emptyList();
        }

        return config.dataList.stream()
                .filter(data -> {
                    for (String field : config.filterFields) {
                        Object value = getFieldValue(data, field);
                        if (value instanceof Long && config.customDeptIds.contains(value)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据部门过滤数据
     */
    private static <T> List<T> filterByDept(DataScopeConfig<T> config) {
        Long deptId = config.loginUser.getUser().getDeptId();
        if (deptId == null) {
            return Collections.emptyList();
        }

        return config.dataList.stream()
                .filter(data -> {
                    for (String field : config.filterFields) {
                        Object value = getFieldValue(data, field);
                        if (value instanceof Long && deptId.equals(value)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * 根据本人过滤数据
     */
    private static <T> List<T> filterBySelf(DataScopeConfig<T> config) {
        Long userId = config.loginUser.getUser().getUserId();
        if (userId == null) {
            return Collections.emptyList();
        }

        return config.dataList.stream()
                .filter(data -> {
                    for (String field : config.filterFields) {
                        Object value = getFieldValue(data, field);
                        if (value instanceof Long && userId.equals(value)) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取字段值
     */
    private static Object getFieldValue(Object obj, String fieldName) {
        try {
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            return obj.getClass().getMethod(getterName).invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
