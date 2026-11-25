package com.qykj.finmaster.sale.fieldwork.domain.dto;

import lombok.Data;

/**
 * 外勤过程记录推送给下发人的通知消息
 */
@Data
public class FieldWorkProgressNotifyDTO {

    /** 外勤任务ID */
    private Long taskId;

    /** 任务编号 */
    private String taskNo;

    /** 客户名称 */
    private String customerName;

    /** 提交人名称 */
    private String submitterName;

    /** 过程描述（取前100字） */
    private String contentPreview;

    /** 提交时间 */
    private String submitTime;

    /** 过程记录ID */
    private Long progressId;
}
