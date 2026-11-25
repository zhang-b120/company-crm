package com.qykj.finmaster.task.service;

import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.task.domain.*;
import com.qykj.finmaster.task.mapper.*;
import com.qykj.finmaster.task.vo.TaskProcessReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

@Service
public class TaskServiceImpl {

    private static final Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskFlowMapper taskFlowMapper;

    @Autowired
    private TaskHandleDetailMapper taskHandleDetailMapper;

    @Autowired
    private TaskItemConfigMapper taskItemConfigMapper;

    @Autowired
    private TaskItemRecordMapper taskItemRecordMapper;

    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;

    @Transactional
    public int addTask(Task task,Long tenantId) {
        try {

            // 检查是否已存在相同订单号的任务
            Task existingTask = taskMapper.selectTaskByOrderNumber(task.getOrderNumber());
            if (existingTask != null) {
                throw new BusinessException("此订单已下发任务单");
            }

            // 设置租户ID
            task.setTenantId(tenantId);

            // 生成任务编号
            task.setTaskNo(generateTaskNumber());
            task.setCurrentStep("验证税密");
            // 设置初始状态
            int result = taskMapper.insertTask(task);

            // === 新增：插入初始流程记录 ===
            if (result > 0) {
                TaskFlow flow = new TaskFlow();
                flow.setTenantId(tenantId); // 设置租户ID
                flow.setTaskId(task.getId());
                flow.setStepName("验证税密");
                flow.setHandlerRole(""); // 如有角色可填写
                flow.setHandler(task.getAcceptPerson());
                flow.setStatus("PENDING");
                flow.setCreateTime(new java.util.Date());
                flow.setRemark("账套已经创建，需验证税密，办税员手机号请使用13969134863，密码为Aa123456");
                taskFlowMapper.insertTaskFlow(flow);
            }

            logger.info("新增任务成功 - 任务ID: {}, 租户ID: {}", task.getId(), tenantId);
            return result;
        } catch (Exception e) {
            logger.error("新增任务失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public int updateTask(Task task,Long tenantId) {
        try {

            // 确保租户ID不被修改
            task.setTenantId(tenantId);

            int result = taskMapper.updateTask(task);
            return result;
        } catch (Exception e) {
            throw e;
        }
    }

    public List<Task> selectTaskList(Task task) {
        try {
            logger.info("查询任务列表 - 不限制租户");

            // 不设置租户ID，查询所有任务
            // task.setTenantId(tenantId);

            List<Task> result = taskMapper.selectTaskList(task);
            logger.info("查询任务列表成功 - 结果数量: {}", result.size());
            return result;
        } catch (Exception e) {
            logger.error("查询任务列表失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Task selectTaskById(Integer id) {
        try {
            logger.info("查询任务详情 - 任务ID: {}", id);
            // 不限制租户，查询所有任务详情
            return taskMapper.selectTaskById(id, null);
        } catch (Exception e) {
            logger.error("查询任务详情失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    public int updateTaskStatus(Integer id, String status, String currentStep) {
        return taskMapper.updateTaskStatus(id, status, currentStep);
    }

    @Transactional
    public void createTaskFlow(Integer taskId, String stepName, String handlerRole, String handler,Long tenantId) {
        try {
            logger.info("创建任务流程 - 任务ID: {}, 步骤: {}, 租户ID: {}", taskId, stepName, tenantId);

            // 创建任务流转记录
            TaskFlow taskFlow = new TaskFlow();
            taskFlow.setTenantId(tenantId); // 设置租户ID
            taskFlow.setTaskId(taskId);
            taskFlow.setStepName(stepName);
            taskFlow.setHandlerRole(handlerRole);
            taskFlow.setHandler(handler);
            taskFlow.setStatus("PENDING");
            taskFlowMapper.insertTaskFlow(taskFlow);

            // 获取该角色的任务项配置
            List<TaskItemConfig> itemConfigs = taskItemConfigMapper.selectTaskItemConfigByRole(handlerRole);

            // 创建任务项记录
            for (TaskItemConfig config : itemConfigs) {
                TaskItemRecord record = new TaskItemRecord();
                record.setTenantId(tenantId); // 设置租户ID
                record.setTaskId(taskId);
                record.setFlowId(taskFlow.getId());
                record.setItemCode(config.getItemCode());
                record.setItemName(config.getItemName());
                record.setStatus("PENDING");
                taskItemRecordMapper.insertTaskItemRecord(record);
            }

            logger.info("创建任务流程成功 - 任务ID: {}, 租户ID: {}", taskId, tenantId);
        } catch (Exception e) {
            logger.error("创建任务流程失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void completeTaskItem(Integer taskId, Integer flowId, String itemCode, String handleResult) {
        // 更新任务项记录
        TaskItemRecord record = taskItemRecordMapper.selectTaskItemRecord(taskId, flowId, itemCode);
        record.setStatus("COMPLETED");
        record.setHandleResult(handleResult);
        record.setHandleTime(new Date());
        taskItemRecordMapper.updateTaskItemRecord(record);

        // 检查是否所有任务项都已完成
        List<TaskItemRecord> records = taskItemRecordMapper.selectTaskItemRecordList(taskId, flowId);
        boolean allCompleted = records.stream().allMatch(r -> "COMPLETED".equals(r.getStatus()));

        if (allCompleted) {
            // 更新任务流转记录（不限制租户）
            TaskFlow taskFlow = taskFlowMapper.selectLatestFlow(taskId, null);
            taskFlow.setStatus("COMPLETED");
            taskFlow.setHandleTime(new Date());
            taskFlowMapper.updateTaskFlow(taskFlow);

//            // 如果是最后一个步骤，更新任务状态为已完成
//            TaskItemConfig config = taskItemConfigMapper.selectTaskItemConfigByCode(itemCode);
//            if ("FINANCE".equals(config.getHandlerRole())) {
//                updateTaskStatus(taskId, "COMPLETED", null, null);
//            }
        }
    }

    public List<TaskFlow> getTaskFlowList(Integer taskId) {
        try {
            logger.info("查询任务流程列表 - 任务ID: {}", taskId);

            // 不限制租户，查询所有任务流程
            List<TaskFlow> result = taskFlowMapper.selectTaskFlowList(taskId, null);
            logger.info("查询任务流程列表成功 - 任务ID: {}, 流程数量: {}", taskId, result.size());
            return result;
        } catch (Exception e) {
            logger.error("查询任务流程列表失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void processTask(TaskProcessReq req,Long tenantId) {
        if ("验证税密".equals(req.getCurrentStep()) && req.getTaxOperatorPhone() != null) {
            // 1. 更新任务主表的当前步骤为财务做账
            Task task = new Task();
            task.setId(req.getTaskId());
            task.setCurrentStep("财务做账");
            task.setStatus("PROCESSING");
            taskMapper.updateTask(task);
            // 2. 更新任务流程中当前流程的状态（不限制租户）
            TaskFlow currentFlow = taskFlowMapper.selectLatestFlow(req.getTaskId(), null);
            if (currentFlow != null) {
                currentFlow.setStatus("COMPLETED");
                currentFlow.setHandleResult(req.getHandleResult());
                currentFlow.setHandleTime(new Date());
                taskFlowMapper.updateTaskFlow(currentFlow);
            }
            // 3. 添加新的任务流程
            TaskFlow newFlow = new TaskFlow();
            newFlow.setTenantId(tenantId); // 设置租户ID
            newFlow.setTaskId(req.getTaskId());
            newFlow.setStepName("财务做账");
            newFlow.setHandlerRole("FINANCE");
            newFlow.setHandler(req.getNextHandler());
            newFlow.setStatus("PENDING");
            newFlow.setCreateTime(new Date());
            newFlow.setRemark(req.getRemark());
            taskFlowMapper.insertTaskFlow(newFlow);
            // 4. 如果是验证税密任务，保存处理详情
            customerCompanyManager.lambdaUpdate()
                    .set(CustomerCompany::getIsVerifyTax, GlobalConstant.Number.ONE_INT)
                    .eq(CustomerCompany::getId, req.getCompanyId())
                    .update();
        } else if ("财务做账".equals(req.getCurrentStep())) {
            // 处理财务做账任务
            // 1. 更新任务流程中当前流程的状态（不限制租户）
            TaskFlow currentFlow = taskFlowMapper.selectLatestFlow(req.getTaskId(), null);
            if (currentFlow != null) {
                currentFlow.setStatus("PROCESSING");
                currentFlow.setHandleResult(req.getHandleResult());
                currentFlow.setHandleTime(new Date());
                currentFlow.setRemark(req.getRemark());
                taskFlowMapper.updateTaskFlow(currentFlow);
            }

            // 2. 更新任务主表状态为处理中
            Task task = new Task();
            task.setId(req.getTaskId());
            task.setStatus("COMPLETED");
            task.setStatus("财务已接收");
            taskMapper.updateTask(task);
        }
    }

    private String generateTaskNumber() {
        // 生成任务编号：TASK + 年月日时分秒 + 4位随机数
        return "TASK" + String.format("%tY%<tm%<td%<tH%<tM%<tS", new Date()) +
               String.format("%04d", (int)(Math.random() * 10000));
    }

    @Transactional
    public int deleteTask(Integer id) {
        try {
            logger.info("删除任务 - 任务ID: {}", id);

            // 先查询任务是否存在（不限制租户）
            Task task = taskMapper.selectTaskById(id, null);
            if (task == null) {
                throw new BusinessException("任务不存在");
            }

            // 删除任务相关的所有数据（不限制租户）
            // 删除任务项记录
            taskItemRecordMapper.deleteTaskItemRecordByTaskId(id, null);

            // 删除任务流程记录
            taskFlowMapper.deleteTaskFlowByTaskId(id, null);

            // 删除任务处理明细
            taskHandleDetailMapper.deleteTaskHandleDetailByTaskId(id, null);

            // 删除主任务
            int result = taskMapper.deleteTask(id);

            logger.info("删除任务成功 - 任务ID: {}", id);
            return result;
        } catch (Exception e) {
            logger.error("删除任务失败: {}", e.getMessage(), e);
            throw e;
        }
    }
}
