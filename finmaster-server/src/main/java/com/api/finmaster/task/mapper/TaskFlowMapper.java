package com.qykj.finmaster.task.mapper;

import com.qykj.finmaster.task.domain.TaskFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskFlowMapper {
    /**
     * 新增任务流转记录
     */
    int insertTaskFlow(TaskFlow taskFlow);

    /**
     * 更新任务流转记录
     */
    int updateTaskFlow(TaskFlow taskFlow);

    /**
     * 查询任务流转记录列表
     */
    List<TaskFlow> selectTaskFlowList(@Param("taskId") Integer taskId, @Param("tenantId") Long tenantId);

    /**
     * 获取最新的流转记录
     */
    TaskFlow selectLatestFlow(@Param("taskId") Integer taskId, @Param("tenantId") Long tenantId);

    /**
     * 根据任务ID删除任务流程记录
     */
    int deleteTaskFlowByTaskId(@Param("taskId") Integer taskId, @Param("tenantId") Long tenantId);
} 