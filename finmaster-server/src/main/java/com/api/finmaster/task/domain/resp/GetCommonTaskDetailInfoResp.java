package com.qykj.finmaster.task.domain.resp;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-08-11
 * @Description
 */
@Data
public class GetCommonTaskDetailInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 410758543849543646L;
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 任务Id
     */
    private Long taskId;

    /**
     * 是否当前流程（0否 1是）
     */
    private Integer isCurrent;

    /**
     * 执行顺序
     */
    private Integer sort;

    /**
     * 编码
     */
    private String processCode;

    /**
     * name
     */
    private String processName;

    /**
     * 处理方式 0-人工, 1-自动
     */
    private Integer handMethod;

    /**
     * 任务数据
     */
    private String taskData;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

    /**
     * 实际完成时间
     */
    private LocalDate realFinishTime;

    /**
     * 状态 字典COMMON_TASK_DETAIL_STATUS
     */
    private String status;

    /**
     * 状态名称 字典COMMON_TASK_DETAIL_STATUS
     */
    private String statusName;

    /**
     * 挂起原因
     */
    private String putUpReason;

}