package com.qykj.finmaster.task.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TaskFlow {
    private Integer id;
    
    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;
    
    private Integer taskId;
    
    private String stepName;
    
    private String handlerRole;
    
    private String handler;
    
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;
    
    private String handleResult;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    private String remark;
} 