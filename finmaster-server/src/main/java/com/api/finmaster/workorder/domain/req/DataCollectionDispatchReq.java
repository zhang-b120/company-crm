package com.qykj.finmaster.workorder.domain.req;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 数据收集任务批量下发：按管家用户名批量创建任务（每人一条）。
 * 任务下属公司列表运行时按管家从 company_service 解析，不在工单表保存。
 */
@Data
public class DataCollectionDispatchReq implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 选中的管家 username 列表（每人一条独立任务） */
    @NotEmpty(message = "请至少选择一名管家")
    private List<String> assignees;

    /** 账期 yyyy-MM-01；缺省时取系统默认（15 号前=上月 1 号，否则当月 1 号） */
    private String period;

    /** 任务截止时间（可选） */
    private Date dueTime;

    /** 备注 */
    private String remark;
}
