package com.qykj.finmaster.task.service;

import com.qykj.finmaster.common.response.Result;
import com.qykj.finmaster.task.domain.Task;
import com.qykj.finmaster.task.mapper.TaskMapper;
import com.qykj.finmaster.task.vo.TaskVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TaskService {

    @Autowired
    private TaskMapper taskMapper;

    /**
     * 将TaskVO转换为Task实体
     */
    private Task convertToEntity(TaskVO vo) {
        if (vo == null) {
            return null;
        }
        Task task = new Task();
        BeanUtils.copyProperties(vo, task);
        return task;
    }

    /**
     * 将Task实体转换为TaskVO
     */
    private TaskVO convertToVO(Task task) {
        if (task == null) {
            return null;
        }
        TaskVO vo = new TaskVO();
        BeanUtils.copyProperties(task, vo);
        return vo;
    }

    /**
     * 新增任务
     */
    @Transactional
    public Result<Void> addTask(TaskVO taskVO) {
        try {
            // 生成任务编号
            taskVO.setTaskNumber("TASK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            // 设置创建时间
            taskVO.setCreateTime(new Date());
            // 设置更新时间
            taskVO.setUpdateTime(new Date());

            Task task = convertToEntity(taskVO);
            int rows = taskMapper.insertTask(task);
            return rows > 0 ? Result.success() : Result.error("新增任务失败");
        } catch (Exception e) {
            return Result.error("新增任务失败：" + e.getMessage());
        }
    }

    /**
     * 更新任务
     */
    @Transactional
    public Result<Void> updateTask(TaskVO taskVO) {
        try {
            // 设置更新时间
            taskVO.setUpdateTime(new Date());
            Task task = convertToEntity(taskVO);
            int rows = taskMapper.updateTask(task);
            return rows > 0 ? Result.success() : Result.error("更新任务失败");
        } catch (Exception e) {
            return Result.error("更新任务失败：" + e.getMessage());
        }
    }

    /**
     * 查询任务列表
     */
    public Result<List<TaskVO>> getTaskList(TaskVO taskVO) {
        try {
            Task task = convertToEntity(taskVO);
            List<Task> list = taskMapper.selectTaskList(task);
            List<TaskVO> voList = list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            return Result.success(voList);
        } catch (Exception e) {
            return Result.error("查询任务列表失败：" + e.getMessage());
        }
    }

    /*
    * 查询任务单
    * */
    public List<TaskVO> list(TaskVO taskVO) {
        Task task = convertToEntity(taskVO);
        List<Task> list = taskMapper.selectTaskList(task);
        return list.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    /*
    * 修改任务单
    * */
    public void edit(TaskVO taskVO) {
        Task task = convertToEntity(taskVO);
        taskMapper.updateTask(task);
    }

    /*
    * 删除任务单
    * */
    public void delete(Integer taskId) {
        taskMapper.deleteTask(taskId);
    }

/*    Test getTest();*/
}
