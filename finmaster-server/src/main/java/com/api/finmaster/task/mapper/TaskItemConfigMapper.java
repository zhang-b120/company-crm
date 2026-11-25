package com.qykj.finmaster.task.mapper;

import com.qykj.finmaster.task.domain.TaskItemConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TaskItemConfigMapper {
    /**
     * 新增任务项配置
     */
    int insertTaskItemConfig(TaskItemConfig config);

    /**
     * 更新任务项配置
     */
    int updateTaskItemConfig(TaskItemConfig config);

    /**
     * 查询任务项配置列表
     */
    List<TaskItemConfig> selectTaskItemConfigList(TaskItemConfig config);

    /**
     * 根据角色查询任务项配置
     */
    List<TaskItemConfig> selectTaskItemConfigByRole(@Param("handlerRole") String handlerRole);

    /**
     * 根据编码查询任务项配置
     */
    TaskItemConfig selectTaskItemConfigByCode(@Param("itemCode") String itemCode);
} 