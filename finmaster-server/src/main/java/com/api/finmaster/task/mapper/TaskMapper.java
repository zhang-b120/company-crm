package com.qykj.finmaster.task.mapper;

import com.qykj.finmaster.task.domain.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskMapper {
    /**
     * 新增任务
     */
    int insertTask(Task task);

    /**
     * 更新任务
     */
    int updateTask(Task task);

    /**
     * 查询任务列表
     */
    List<Task> selectTaskList(Task task);

    /**
     * 根据ID查询任务
     */
    Task selectTaskById(@Param("id") Integer id, @Param("tenantId") Long tenantId);

    /**
     * 根据订单号查询任务
     */
    Task selectTaskByOrderNumber(@Param("orderNumber") String orderNumber);

    /**
     * 更新任务状态
     */
    int updateTaskStatus(@Param("id") Integer id, @Param("status") String status, @Param("currentStep") String currentStep);

    /**
     * 删除任务
     */
    int deleteTask(@Param("id") Integer id);
}
