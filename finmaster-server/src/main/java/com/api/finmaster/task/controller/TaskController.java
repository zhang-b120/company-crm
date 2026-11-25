package com.qykj.finmaster.task.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.system.tenant.service.TenantServiceImpl;
import com.qykj.finmaster.task.domain.Task;
import com.qykj.finmaster.task.service.TaskServiceImpl;
import com.qykj.finmaster.task.vo.TaskProcessReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/task")
public class TaskController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskServiceImpl taskService;
    @Autowired
    private TenantServiceImpl tenantServiceImpl;

    /**
     * 新增任务
     */
    @PostMapping
    public AjaxResult addTask(@RequestBody Task task) {
        try {
            Long tenantId = getTenantId();
            logger.info("新增任务 - 租户ID: {}", tenantId);

            // 设置租户ID
            task.setTenantId(tenantId);

            int result = taskService.addTask(task,tenantId);
            logger.info("新增任务成功 - 任务ID: {}, 租户ID: {}", task.getId(), tenantId);
            return AjaxResult.success(result);
        } catch (Exception e) {
            logger.error("新增任务失败: {}", e.getMessage(), e);
            return AjaxResult.error("新增任务失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务
     */
    @PutMapping
    public AjaxResult updateTask(@RequestBody Task task) {
        try {
            Long tenantId = getTenantId();
            logger.info("更新任务 - 任务ID: {}, 租户ID: {}", task.getId(), tenantId);

            // 确保租户ID不被修改
            task.setTenantId(tenantId);

            int result = taskService.updateTask(task,tenantId);
            logger.info("更新任务成功 - 任务ID: {}, 租户ID: {}", task.getId(), tenantId);
            return AjaxResult.success(result);
        } catch (Exception e) {
            logger.error("更新任务失败: {}", e.getMessage(), e);
            return AjaxResult.error("更新任务失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务列表
     */
    @GetMapping("/list")
    public AjaxResult list(Task task) {
        try {
            logger.info("查询任务列表 - 不限制租户");

            // 不设置租户ID，查询所有任务
            // task.setTenantId(tenantId);
            Long tenantId = getTenantId();
            task.setTenantId(tenantId);
            var result = taskService.selectTaskList(task);
            logger.info("查询任务列表成功 - 结果数量: {}", result.size());
            return AjaxResult.success(result);
        } catch (Exception e) {
            logger.error("查询任务列表失败: {}", e.getMessage(), e);
            return AjaxResult.error("查询任务列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Integer id) {
        try {
            logger.info("获取任务详情 - 任务ID: {}", id);

            var result = taskService.selectTaskById(id);
            logger.info("获取任务详情成功 - 任务ID: {}", id);
            return AjaxResult.success(result);
        } catch (Exception e) {
            logger.error("获取任务详情失败: {}", e.getMessage(), e);
            return AjaxResult.error("获取任务详情失败: " + e.getMessage());
        }
    }

    /**
     * 创建任务流程
     */
    @PostMapping("/flow")
    public AjaxResult createFlow(@RequestParam Integer taskId, @RequestParam String stepName,
                          @RequestParam String handlerRole, @RequestParam String handler) {
        try {
            logger.info("创建任务流程 - 任务ID: {}, 步骤: {}", taskId, stepName);

            taskService.createTaskFlow(taskId, stepName, handlerRole, handler,getTenantId());
            logger.info("创建任务流程成功 - 任务ID: {}", taskId);
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error("创建任务流程失败: {}", e.getMessage(), e);
            return AjaxResult.error("创建任务流程失败: " + e.getMessage());
        }
    }

    /**
     * 完成任务项
     */
    @PostMapping("/item/complete")
    public AjaxResult completeItem(@RequestParam Integer taskId, @RequestParam Integer flowId,
                            @RequestParam String itemCode, @RequestParam String handleResult) {
        try {
            logger.info("完成任务项 - 任务ID: {}, 流程ID: {}, 项目代码: {}",
                taskId, flowId, itemCode);

            taskService.completeTaskItem(taskId, flowId, itemCode, handleResult);
            logger.info("完成任务项成功 - 任务ID: {}", taskId);
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error("完成任务项失败: {}", e.getMessage(), e);
            return AjaxResult.error("完成任务项失败: " + e.getMessage());
        }
    }

    /**
     * 查询指定任务的所有流程记录
     */
    @GetMapping("/flow/list/{taskId}")
    public AjaxResult getTaskFlowList(@PathVariable Integer taskId) {
        try {
            logger.info("查询任务流程列表 - 任务ID: {}", taskId);

            var result = taskService.getTaskFlowList(taskId);
            logger.info("查询任务流程列表成功 - 任务ID: {}, 流程数量: {}", taskId, result.size());
            return AjaxResult.success(result);
        } catch (Exception e) {
            logger.error("查询任务流程列表失败: {}", e.getMessage(), e);
            return AjaxResult.error("查询任务流程列表失败: " + e.getMessage());
        }
    }

    /**
     * 处理任务（包含更新任务主表、更新当前流程状态、创建新流程）
     */
    @PostMapping("/process")
    public AjaxResult processTask(@RequestBody TaskProcessReq req) {
        try {
            Long tenantId= getTenantId();
            logger.info("处理任务 - 任务ID: {}, 当前步骤: {}",
                req.getTaskId(), req.getCurrentStep());

            taskService.processTask(req,tenantId);
            logger.info("处理任务成功 - 任务ID: {}", req.getTaskId());
            return AjaxResult.success();
        } catch (Exception e) {
            logger.error("处理任务失败: {}", e.getMessage(), e);
            return AjaxResult.error("处理任务失败: " + e.getMessage());
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public AjaxResult deleteTask(@PathVariable Integer id) {
        try {
            logger.info("删除任务 - 任务ID: {}", id);

            int result = taskService.deleteTask(id);
            logger.info("删除任务成功 - 任务ID: {}", id);
            return AjaxResult.success(result);
        } catch (Exception e) {
            logger.error("删除任务失败: {}", e.getMessage(), e);
            return AjaxResult.error("删除任务失败: " + e.getMessage());
        }
    }
}
