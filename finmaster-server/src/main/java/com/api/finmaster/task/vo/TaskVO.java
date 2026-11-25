package com.qykj.finmaster.task.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TaskVO {
    private Integer id;
    
    private String taskNumber;
    
    private String orderNumber;
    
    private String taskSource;
    
    private String submitPerson;
    
    private String taskType;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    
    private String illustrate;
    
    private String dispose;

    private String customerName;
    
    private String companyName;
    
    private Integer companyId;
    
    private String taxNumber;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date effectiveDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;
    
    private String productNames;
    
    private String companyType;
    
    private String accountingStandard;
    
    private String province;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
