package com.qykj.finmaster.task.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TaskHandleDetail {
    private Integer id;
    
    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;
    
    private Integer taskId;
    
    private Integer flowId;
    
    private String handlerRole;
    
    private String taxOperatorPhone;
    
    private String taxOperatorPassword;
    
    private String provinceCheckResult;
    
    private String handleContent;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    private String createBy;
} 