package com.qykj.finmaster.task.domain.req;

import com.qykj.finmaster.task.domain.dto.CommonDetailInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-09
 * @Description
 */
@Data
public class UpdateCommonTaskReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 6744458488676576263L;

    private Long id;
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
     * 公司ID
     */
    private Long companyId;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

    /**
     * 期望完成时间
     */
    private LocalDate wishFinishTime;

    /**
     * 状态 字典COMMON_TASK_STATUS
     */
    private String status;

    private List<CommonDetailInfo> detailInfoList;
}