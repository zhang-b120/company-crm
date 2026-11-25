package com.qykj.finmaster.oa.approval.domain.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.oa.approval.domain.ApprovalRecord;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 审批实例详情响应
 */
@Data
public class ApprovalInstanceResp {

    /** 实例ID */
    private Long id;

    /** 业务类型 */
    private String businessType;

    /** 业务ID */
    private Long businessId;

    /** 审批标题 */
    private String title;

    /** 申请人用户名 */
    private String applicant;

    /** 申请人姓名 */
    private String applicantName;

    /** 审批状态 */
    private String status;

    /** 当前节点序号 */
    private Integer currentNode;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 审批记录列表 */
    private List<ApprovalRecord> records;
}
