package com.qykj.finmaster.common.core.domain.page;

import com.github.pagehelper.PageHelper;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.common.utils.sql.SqlUtil;
import com.qykj.finmaster.security.LoginUser;
import lombok.Data;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-06-07
 * @Description
 */
@Data
public class PageEntity implements Serializable {
    private static final long serialVersionUID = 6515371629982667366L;

    /** 当前记录起始索引 */
    private Integer pageNum;

    /** 每页显示记录数 */
    private Integer pageSize;

    /** 排序列 */
    private String orderByColumn;

    /** 排序的方向desc或者asc */
    private String isAsc = "asc";

    private List<String> dataScopeUserIds;

    public void startPage() {
        Integer pageNum = this.pageNum;
        Integer pageSize = this.pageSize;
        if (StringUtils.isNotNull(pageNum) && StringUtils.isNotNull(pageSize)) {
            String orderBy = SqlUtil.escapeOrderBySql(this.orderByColumn);
            PageHelper.startPage(pageNum, pageSize, orderBy);
        }
    }

    public Map<String, Object> toBeanMap() {
        Map<String, Object> map = new HashMap<>();
        BeanWrapper beanWrapper = new BeanWrapperImpl(this);
        for (java.beans.PropertyDescriptor pd : beanWrapper.getPropertyDescriptors()) {
            String propertyName = pd.getName();
            if (!"class".equals(propertyName)) {
                Object value = beanWrapper.getPropertyValue(propertyName);
                if (value != null) {
                    map.put(propertyName, value);
                }
            }
        }
        return map;
    }

    public List<String> getDataScopeUserIds() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        // 检查principal是否为LoginUser类型，避免ClassCastException
        if (principal instanceof LoginUser) {
            LoginUser loginUser = (LoginUser) principal;
            return loginUser.getDataScopeUserIds();
        }

        // 如果是匿名用户或其他类型，返回null（表示不进行数据权限过滤）
        return null;
    }
}