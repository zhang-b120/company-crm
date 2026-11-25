package com.qykj.finmaster.sale.fieldwork.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class GetFieldWorkTaskListReq extends PageEntity {

    /** 客户名称（模糊查询） */
    private String customerName;

    /** 任务类型 */
    private String taskType;

    /** 任务状态 */
    private String status;

    /** 计划开始时间 - 起始（yyyy-MM-dd HH:mm:ss） */
    private Date start;

    /** 计划开始时间 - 截止（yyyy-MM-dd HH:mm:ss） */
    private Date end;
}
