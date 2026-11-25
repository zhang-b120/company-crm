package com.qykj.finmaster.task.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TaskItemConfig {
    private Integer id;
    
    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;
    
    private String itemCode;
    
    private String itemName;
    
    private String itemType;
    
    private String handlerRole;
    
    private Integer sortOrder;
    
    private String status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
} 