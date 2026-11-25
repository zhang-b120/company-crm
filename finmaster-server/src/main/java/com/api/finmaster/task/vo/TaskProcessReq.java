package com.qykj.finmaster.task.vo;

import lombok.Data;

import java.util.List;

@Data
public class TaskProcessReq {
    private Integer taskId;
    private String currentStep;
    private String handleResult;
    private String remark;
    private String nextHandler;
    private List<String> completedItems;
    private String openId;
    private String taxNumber;
    private Integer companyId;
    
    // 验证税密任务特殊字段
    private String taxOperatorPhone;
    private String taxOperatorPassword;
    private String verificationStatus;
} 