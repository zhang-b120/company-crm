package com.qykj.finmaster.task.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-11
 * @Description
 */
@Data
public class GetCommonTaskInfoResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 4922599988491216346L;

    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 任务单号
     */
    private String taskNo;

    /**
     * 服务ID
     */
    private Long serviceId;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 来源单据号
     */
    private String sourceNumber;

    /**
     * 来源单据id
     */
    private Long sourceId;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 公司税号
     */
    private String taxNumber;

    /**
     * 公司类型
     */
    private String companyType;

    /**
     * 公司类型名称
     */
    private String companyTypeName;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

    /**
     * 当前负责人
     */
    private String currentOwnerUser;

    /**
     * 当前负责人名称
     */
    private String currentOwnerUserName;

    /**
     * 创建人
     */
    private String createBy;

    /**
     * 创建人
     */
    private String createByName;

    /**
     * 期望完成时间
     */
    private LocalDate wishFinishTime;

    /**
     * 实际完成时间
     */
    private LocalDate realFinishTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 状态 字典COMMON_TASK_STATUS
     */
    private String status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 任务数据
     */
    private String taskData;

    /**
     * 挂起原因
     */
    private String putUpReason;

    /**
     * 取消原因
     */
    private String cancelReason;

    private List<GetCommonTaskDetailInfoResp> detailInfoRespList;
}