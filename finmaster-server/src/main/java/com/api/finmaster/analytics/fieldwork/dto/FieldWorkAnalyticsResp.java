package com.qykj.finmaster.analytics.fieldwork.dto;

import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class FieldWorkAnalyticsResp {

    @Data
    public static class OverviewResp {
        private String scope;
        private Long totalTasks;
        private Long completedTasks;
        private Long pendingTasks;
        private Long inProgressTasks;
        private Long cancelledTasks;
        private BigDecimal completionRate;
        private BigDecimal cancellationRate;
        /** 平均实际工时（分钟） */
        private BigDecimal avgActualDuration;
        /** 平均到场延迟（分钟，= actual_start_time - planned_start_time） */
        private BigDecimal avgArrivalDelayMin;
        /** 按任务类型分布 */
        private List<TaskTypeItem> taskTypeDist;
    }

    @Data
    public static class TaskTypeItem {
        private String taskType;
        private Long count;
        private BigDecimal avgDuration;
    }

    @Data
    public static class RankItem {
        private Long assigneeId;
        @Excel(title = "外勤人", sort = 1)
        private String assigneeName;
        @Excel(title = "任务数", sort = 2)
        private Long totalTasks;
        @Excel(title = "已完成", sort = 3)
        private Long completedTasks;
        @Excel(title = "完成率(%)", sort = 4)
        private BigDecimal completionRate;
        @Excel(title = "总工时(分)", sort = 5)
        private BigDecimal totalDurationMin;
        @Excel(title = "平均延迟(分)", sort = 6)
        private BigDecimal avgArrivalDelayMin;
    }
}
