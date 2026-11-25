package com.qykj.finmaster.system.bizConfig.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.bizConfig.entity.SysBizConfig;
import com.qykj.finmaster.system.bizConfig.manager.impl.SysBizConfigManagerImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 业务配置 Service（V2.0.23）
 * - 列表/取值/批量更新
 * - 提供 getInt / getBool / getString 快捷方法供业务代码使用
 */
@Service
public class SysBizConfigService {

    @Autowired
    private SysBizConfigManagerImpl manager;

    /** 按模块查所有配置项（按 sortOrder 排序） */
    public List<SysBizConfig> listByModule(String module) {
        return manager.lambdaQuery()
                .eq(SysBizConfig::getModule, module)
                .orderByAsc(SysBizConfig::getSortOrder)
                .list();
    }

    /** 列出所有模块（去重） */
    public List<String> listModules() {
        return manager.lambdaQuery()
                .select(SysBizConfig::getModule)
                .groupBy(SysBizConfig::getModule)
                .list().stream()
                .map(SysBizConfig::getModule)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** 按 key 取当前值（如果当前值为空，取默认值） */
    public String getString(String configKey) {
        SysBizConfig c = manager.lambdaQuery()
                .eq(SysBizConfig::getConfigKey, configKey)
                .one();
        if (c == null) return null;
        return StringUtils.isNotBlank(c.getConfigValue()) ? c.getConfigValue() : c.getDefaultValue();
    }

    public String getString(String configKey, String fallback) {
        String v = getString(configKey);
        return v != null ? v : fallback;
    }

    /** 取整型 */
    public Integer getInt(String configKey, Integer fallback) {
        String v = getString(configKey);
        if (v == null) return fallback;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return fallback; }
    }

    /** 取布尔（"1"/"true" 视作 true） */
    public Boolean getBool(String configKey, Boolean fallback) {
        String v = getString(configKey);
        if (v == null) return fallback;
        v = v.trim().toLowerCase();
        if ("1".equals(v) || "true".equals(v) || "yes".equals(v)) return true;
        if ("0".equals(v) || "false".equals(v) || "no".equals(v)) return false;
        return fallback;
    }

    // ===================== 复合配置（value_type = 'group'）读取 =====================
    // group 类型的 config_value 是 JSON 对象字符串，例：{"enabled":"1","no_follow_days":"14",...}
    // 使用者通过 groupKey + fieldKey 取值，避免拼接 config_key 并重复查 DB 的问题。

    /**
     * 取 group 类型配置的整个子字段 Map；不存在或解析失败返回空 Map
     */
    public Map<String, Object> getGroupMap(String groupKey) {
        SysBizConfig c = manager.lambdaQuery()
                .eq(SysBizConfig::getConfigKey, groupKey)
                .one();
        if (c == null) return Collections.emptyMap();
        String raw = StringUtils.isNotBlank(c.getConfigValue()) ? c.getConfigValue() : c.getDefaultValue();
        if (StringUtils.isBlank(raw)) return Collections.emptyMap();
        try {
            JSONObject obj = JSON.parseObject(raw);
            return obj != null ? obj.getInnerMap() : Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    /** 取 group 子字段字符串 */
    public String getGroupString(String groupKey, String fieldKey, String fallback) {
        Object v = getGroupMap(groupKey).get(fieldKey);
        return v == null ? fallback : String.valueOf(v);
    }

    /** 取 group 子字段整型 */
    public Integer getGroupInt(String groupKey, String fieldKey, Integer fallback) {
        String v = getGroupString(groupKey, fieldKey, null);
        if (v == null) return fallback;
        try { return Integer.parseInt(v.trim()); } catch (Exception e) { return fallback; }
    }

    /** 取 group 子字段布尔（"1"/"true" → true） */
    public Boolean getGroupBool(String groupKey, String fieldKey, Boolean fallback) {
        String v = getGroupString(groupKey, fieldKey, null);
        if (v == null) return fallback;
        v = v.trim().toLowerCase();
        if ("1".equals(v) || "true".equals(v) || "yes".equals(v)) return true;
        if ("0".equals(v) || "false".equals(v) || "no".equals(v)) return false;
        return fallback;
    }

    /** 批量更新（前端"保存修改"按钮一次提交多项） */
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchUpdate(List<SysBizConfig> updates, LoginUser loginUser) {
        if (updates == null || updates.isEmpty()) return true;
        Date now = new Date();
        String operator = loginUser != null ? loginUser.getRealName() : "system";
        for (SysBizConfig u : updates) {
            if (u.getId() == null) continue;
            manager.lambdaUpdate()
                    .set(SysBizConfig::getConfigValue, u.getConfigValue())
                    .set(SysBizConfig::getUpdateBy, operator)
                    .set(SysBizConfig::getUpdateTime, now)
                    .eq(SysBizConfig::getId, u.getId())
                    .update();
        }
        return true;
    }
}
