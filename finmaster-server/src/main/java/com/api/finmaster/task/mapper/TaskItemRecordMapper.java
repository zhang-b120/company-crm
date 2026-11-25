package com.qykj.finmaster.task.mapper;

import com.qykj.finmaster.task.domain.TaskItemRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskItemRecordMapper {
    /**
     * 新增任务项记录
     */
    int insertTaskItemRecord(TaskItemRecord record);

    /**
     * 更新任务项记录
     */
    int updateTaskItemRecord(TaskItemRecord record);

    /**
     * 查询任务项记录列表
     */
    List<TaskItemRecord> selectTaskItemRecordList(@Param("taskId") Integer taskId, @Param("flowId") Integer flowId);

    /**
     * 查询任务项记录
     */
    TaskItemRecord selectTaskItemRecord(@Param("taskId") Integer taskId, @Param("flowId") Integer flowId, 
                                      @Param("itemCode") String itemCode);

    /**
     * 根据流程ID删除任务项记录
     */
    int deleteTaskItemRecordByFlowId(@Param("flowId") Integer flowId);

    /**
     * 根据任务ID删除任务项记录
     */
    int deleteTaskItemRecordByTaskId(@Param("taskId") Integer taskId, @Param("tenantId") Long tenantId);
} 