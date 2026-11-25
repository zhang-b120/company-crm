package com.qykj.finmaster.task.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.qykj.finmaster.security.LoginUser;
import lombok.Data;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.List;

@Data
public class Task {
    private Integer id;
    
    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;
    
    private String taskNo;
    
    private String orderNumber;
    
    private String taskSource;
    
    private String submitPerson;
    
    private String customerName;
    
    private String companyName;
    
    private Integer companyId;
    
    private String taxNumber;
    
    private String taskType;

    private String acceptPerson;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date effectiveDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;
    
    private String productNames;
    
    private String companyType;
    
    private String accountingStandard;
    
    private String province;
    
    private String currentStep;
    
    private String illustrate;

    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @TableField(exist = false)
    private List<String> dataScopeUserIds;

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