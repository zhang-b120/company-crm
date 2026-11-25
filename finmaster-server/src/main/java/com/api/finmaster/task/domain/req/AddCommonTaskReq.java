package com.qykj.finmaster.task.domain.req;

import com.baomidou.mybatisplus.annotation.TableField;
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
public class AddCommonTaskReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 609541877140715131L;

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
     * 期望完成时间（保留兼容，优先使用 scheduleSlotId）
     */
    private LocalDate wishFinishTime;

    /**
     * 预约的号源ID（通过排班系统选择）
     */
    private Long scheduleSlotId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 任务数据
     */
    private String taskData;

    private List<CommonDetailInfo> detailInfoList;
}