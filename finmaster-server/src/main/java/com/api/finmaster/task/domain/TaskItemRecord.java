package com.qykj.finmaster.task.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TaskItemRecord {
    private Integer id;
    
    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;
    
    private Integer taskId;
    
    private Integer flowId;
    
    private String itemCode;
    
    private String itemName;
    
    private String status;
    
    private String handleResult;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date handleTime;
    
    private String handler;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
} 