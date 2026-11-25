package com.qykj.finmaster.task.mapper;

import com.qykj.finmaster.task.domain.TaskHandleDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskHandleDetailMapper {
    /**
     * 新增任务处理详情
     */
    int insertTaskHandleDetail(TaskHandleDetail detail);

    /**
     * 更新任务处理详情
     */
    int updateTaskHandleDetail(TaskHandleDetail detail);

    /**
     * 查询任务处理详情
     */
    TaskHandleDetail selectTaskHandleDetail(@Param("taskId") Integer taskId, @Param("flowId") Integer flowId);

    /**
     * 查询任务的所有处理详情
     */
    List<TaskHandleDetail> selectTaskHandleDetailList(@Param("taskId") Integer taskId);

    /**
     * 根据任务ID删除任务处理详情
     */
    int deleteTaskHandleDetailByTaskId(@Param("taskId") Integer taskId, @Param("tenantId") Long tenantId);
} 