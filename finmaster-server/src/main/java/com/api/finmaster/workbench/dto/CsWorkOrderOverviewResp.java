package com.qykj.finmaster.workbench.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 客服部工单总览返回。工单 74 / CSWO-01。
 * 纯只读统计：按人状态分布 + 超时 + 挂起原因总览 + KPI 汇总。
 */
@Data
public class CsWorkOrderOverviewResp {

    /** 部门名（客服部） */
    private String deptName;

    /** 生成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date generatedAt;

    /** 部门汇总 KPI：memberCount/active/pending/running/overduePending/overdueRunning/suspended/monthDone/stale */
    private Map<String, Object> kpi;

    /** 按客服人员的工单状态分布（含 0 工单成员，已补 0） */
    private List<Map<String, Object>> members;

    /** 待处理超时明细 Top（status=PENDING） */
    private List<Map<String, Object>> overduePendingTop;

    /** 处理中超时明细 Top（status=RUNNING） */
    private List<Map<String, Object>> overdueRunningTop;

    /** 挂起原因汇总（饼图用）：每项 {name(中文), value(数量), reasonCode} */
    private List<Map<String, Object>> suspendReasons;

    /** 挂起单明细（planned_resume_time 已过期者 resumeOverdue=1，前端置顶标红） */
    private List<Map<String, Object>> suspendDetail;
}
