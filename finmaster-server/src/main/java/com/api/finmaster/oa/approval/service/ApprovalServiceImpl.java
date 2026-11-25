package com.qykj.finmaster.oa.approval.service;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.oa.approval.domain.*;
import com.qykj.finmaster.oa.approval.domain.req.ApproveReq;
import com.qykj.finmaster.oa.approval.domain.req.SaveDefinitionReq;
import com.qykj.finmaster.oa.approval.domain.resp.ApprovalInstanceResp;
import com.qykj.finmaster.oa.approval.manager.impl.*;
import com.qykj.finmaster.oa.expense.domain.OaExpense;
import com.qykj.finmaster.oa.expense.manager.impl.OaExpenseManagerImpl;
import com.qykj.finmaster.oa.expense.service.ExpenseServiceImpl;
import com.qykj.finmaster.oa.leave.domain.OaLeave;
import com.qykj.finmaster.oa.leave.manager.impl.OaLeaveManagerImpl;
import com.qykj.finmaster.oa.leave.service.LeaveServiceImpl;
import com.qykj.finmaster.oa.payment.domain.OaPayment;
import com.qykj.finmaster.oa.payment.manager.impl.OaPaymentManagerImpl;
import com.qykj.finmaster.oa.payment.service.PaymentServiceImpl;
import com.qykj.finmaster.oa.trip.domain.OaBusinessTrip;
import com.qykj.finmaster.oa.trip.manager.impl.OaBusinessTripManagerImpl;
import com.qykj.finmaster.oa.trip.service.BusinessTripServiceImpl;
import com.qykj.finmaster.oa.makeup.service.MakeupClockinServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.department.entity.Department;
import com.qykj.finmaster.system.department.manager.impl.DepartmentManagerImpl;
import com.qykj.finmaster.system.user.entity.UserDepartment;
import com.qykj.finmaster.system.user.manager.impl.UserDepartmentManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 审批服务实现类 - 核心审批引擎（支持会签/或签/条件分支/撤回/抄送）
 */
@Slf4j
@Service
public class ApprovalServiceImpl {

    @Autowired
    private ApprovalDefinitionManagerImpl definitionManager;

    @Autowired
    private ApprovalInstanceManagerImpl instanceManager;

    @Autowired
    private ApprovalRecordManagerImpl recordManager;

    @Autowired
    private OaApprovalCCManagerImpl ccManager;

    @Autowired
    private UserDepartmentManagerImpl userDepartmentManager;

    @Autowired
    private DepartmentManagerImpl departmentManager;

    // 用于条件评估的 Manager（直接注入，无循环依赖）
    @Autowired
    private OaExpenseManagerImpl expenseManager;

    @Autowired
    private OaPaymentManagerImpl paymentManager;

    @Autowired
    private OaLeaveManagerImpl leaveManager;

    // 业务回调（使用 @Lazy 打破循环依赖）
    @Lazy
    @Autowired
    private ExpenseServiceImpl expenseService;

    @Lazy
    @Autowired
    private PaymentServiceImpl paymentService;

    @Lazy
    @Autowired
    private LeaveServiceImpl leaveService;

    @Lazy
    @Autowired
    private BusinessTripServiceImpl businessTripService;

    @Lazy
    @Autowired
    private MakeupClockinServiceImpl makeupClockinService;

    @Autowired
    private OaApprovalDelegationManagerImpl delegationManager;

    @Autowired
    private OaApprovalReminderManagerImpl reminderManager;

    @Autowired
    private OaBusinessTripManagerImpl businessTripManager;

    // =========================================================
    // 审批定义管理
    // =========================================================

    /**
     * 保存/更新审批定义
     */
    @Transactional(rollbackFor = Exception.class)
    public Long saveDefinition(SaveDefinitionReq req, Long tenantId, LoginUser loginUser) {
        ApprovalDefinition definition;
        if (req.getId() != null) {
            definition = definitionManager.getById(req.getId());
            if (definition == null) {
                throw new BusinessException("审批定义不存在");
            }
            definition.setUpdateBy(loginUser.getUsername());
            definition.setUpdateByName(loginUser.getRealName());
            definition.setUpdateTime(new Date());
        } else {
            definition = new ApprovalDefinition();
            definition.setTenantId(tenantId);
            definition.setCreateBy(loginUser.getUsername());
            definition.setCreateByName(loginUser.getRealName());
            definition.setCreateTime(new Date());
        }
        definition.setName(req.getName());
        definition.setType(req.getType());
        definition.setNodesJson(req.getNodesJson());
        definition.setEnabled(req.getEnabled() != null && req.getEnabled() ? 1 : 0);

        if (req.getId() != null) {
            definitionManager.updateById(definition);
        } else {
            definitionManager.save(definition);
        }
        return definition.getId();
    }

    /**
     * 获取审批定义列表
     */
    public List<ApprovalDefinition> getDefinitionList(Long tenantId) {
        LambdaQueryWrapper<ApprovalDefinition> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApprovalDefinition::getTenantId, tenantId);
        wrapper.orderByDesc(ApprovalDefinition::getCreateTime);
        return definitionManager.list(wrapper);
    }

    /**
     * 删除审批定义
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDefinition(Long id) {
        return definitionManager.removeById(id);
    }

    // =========================================================
    // 审批流程核心
    // =========================================================

    /**
     * 提交审批
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param title        审批标题
     * @param tenantId     租户ID
     * @param loginUser    当前登录用户
     * @return 审批实例ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(String businessType, Long businessId, String title, Long tenantId, LoginUser loginUser) {
        // 查找该业务类型对应的启用审批定义
        LambdaQueryWrapper<ApprovalDefinition> defWrapper = Wrappers.lambdaQuery();
        defWrapper.eq(ApprovalDefinition::getTenantId, tenantId);
        defWrapper.eq(ApprovalDefinition::getType, businessType);
        defWrapper.eq(ApprovalDefinition::getEnabled, 1);
        defWrapper.last("LIMIT 1");
        ApprovalDefinition definition = definitionManager.getOne(defWrapper);

        if (definition == null) {
            throw new BusinessException("未找到启用的审批定义，业务类型：" + businessType);
        }

        // 解析审批节点并排序
        List<ApprovalNodeConfig> nodes = parseAndSortNodes(definition.getNodesJson());
        if (nodes.isEmpty()) {
            throw new BusinessException("审批定义未配置审批节点");
        }

        // 创建审批实例
        ApprovalInstance instance = new ApprovalInstance();
        instance.setTenantId(tenantId);
        instance.setDefinitionId(definition.getId());
        instance.setBusinessType(businessType);
        instance.setBusinessId(businessId);
        instance.setTitle(title);
        instance.setApplicant(loginUser.getUsername());
        instance.setApplicantName(loginUser.getRealName());
        instance.setStatus("PENDING");
        instance.setCurrentNode(0); // 临时值，待确定第一个有效节点后更新
        instance.setCreateBy(loginUser.getUsername());
        instance.setCreateByName(loginUser.getRealName());
        instance.setCreateTime(new Date());
        instanceManager.save(instance);

        // 找第一个条件匹配的节点
        ApprovalNodeConfig firstNode = findFirstMatchingNode(nodes, businessType, businessId);
        if (firstNode == null) {
            // 所有节点条件均不满足，直接通过
            instance.setStatus("APPROVED");
            instance.setCurrentNode(0);
            instanceManager.updateById(instance);
            notifyBusinessStatus(instance, "APPROVED");
            log.info("所有节点条件不满足，审批自动通过，实例ID: {}", instance.getId());
            return instance.getId();
        }

        // 更新实例的当前节点
        instance.setCurrentNode(firstNode.getSort());
        instanceManager.updateById(instance);

        // 创建第一个节点的审批记录
        createNodeRecords(instance, firstNode, tenantId, loginUser.getUsername(), loginUser.getRealName());

        log.info("审批提交成功，实例ID: {}，业务类型: {}，首节点: {}", instance.getId(), businessType, firstNode.getName());
        return instance.getId();
    }

    /**
     * 审批/拒绝
     */
    @Transactional(rollbackFor = Exception.class)
    public String approve(ApproveReq req, Long tenantId, LoginUser loginUser) {
        ApprovalInstance instance = instanceManager.getById(req.getInstanceId());
        if (instance == null) {
            throw new BusinessException("审批实例不存在");
        }
        if (!"PENDING".equals(instance.getStatus())) {
            throw new BusinessException("该审批已处理，当前状态：" + instance.getStatus());
        }

        // 查找当前用户在当前节点的待审批记录
        LambdaQueryWrapper<ApprovalRecord> recordWrapper = Wrappers.lambdaQuery();
        recordWrapper.eq(ApprovalRecord::getInstanceId, req.getInstanceId());
        recordWrapper.eq(ApprovalRecord::getNodeSort, instance.getCurrentNode());
        recordWrapper.eq(ApprovalRecord::getApprover, loginUser.getUsername());
        recordWrapper.isNull(ApprovalRecord::getAction);
        recordWrapper.eq(ApprovalRecord::getIsCc, 0);
        ApprovalRecord currentRecord = recordManager.getOne(recordWrapper);

        if (currentRecord == null) {
            throw new BusinessException("未找到您的待审批记录，或已处理");
        }

        // 更新审批记录
        currentRecord.setAction(req.getAction());
        currentRecord.setComment(req.getComment());
        currentRecord.setActionTime(new Date());
        currentRecord.setUpdateBy(loginUser.getUsername());
        currentRecord.setUpdateByName(loginUser.getRealName());
        currentRecord.setUpdateTime(new Date());
        recordManager.updateById(currentRecord);

        if ("REJECT".equals(req.getAction())) {
            // 拒绝：作废同节点其他待处理记录，标记实例为已拒绝
            voidOtherPendingRecords(req.getInstanceId(), instance.getCurrentNode(), currentRecord.getId());
            instance.setStatus("REJECTED");
            instance.setUpdateBy(loginUser.getUsername());
            instance.setUpdateByName(loginUser.getRealName());
            instance.setUpdateTime(new Date());
            instanceManager.updateById(instance);
            notifyBusinessStatus(instance, "REJECTED");
            log.info("审批被拒绝，实例ID: {}", instance.getId());
            return "REJECTED";
        }

        if (!"APPROVE".equals(req.getAction())) {
            throw new BusinessException("无效的审批动作：" + req.getAction());
        }

        // 判断签署类型
        String signType = currentRecord.getSignType() != null ? currentRecord.getSignType() : "AND";

        if ("OR".equals(signType)) {
            // 或签：当前人通过即推进，作废同节点其他待处理记录
            voidOtherPendingRecords(req.getInstanceId(), instance.getCurrentNode(), currentRecord.getId());
            return advanceToNextNode(instance, tenantId, loginUser.getUsername(), loginUser.getRealName());
        } else {
            // 会签（AND）：检查当前节点所有审批人是否全部通过
            boolean allApproved = checkAllApproved(req.getInstanceId(), instance.getCurrentNode());
            if (allApproved) {
                return advanceToNextNode(instance, tenantId, loginUser.getUsername(), loginUser.getRealName());
            }
            log.info("会签节点 {} 等待其他审批人，实例ID: {}", instance.getCurrentNode(), instance.getId());
            return "PENDING";
        }
    }

    /**
     * 撤回审批（申请人撤回 PENDING 状态的申请）
     */
    @Transactional(rollbackFor = Exception.class)
    public void recall(Long instanceId, LoginUser loginUser) {
        ApprovalInstance instance = instanceManager.getById(instanceId);
        if (instance == null) {
            throw new BusinessException("审批实例不存在");
        }
        if (!"PENDING".equals(instance.getStatus())) {
            throw new BusinessException("只有审批中的申请才能撤回");
        }
        if (!loginUser.getUsername().equals(instance.getApplicant())) {
            throw new BusinessException("只有申请人才能撤回");
        }

        // 作废所有待处理的审批记录
        LambdaQueryWrapper<ApprovalRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApprovalRecord::getInstanceId, instanceId);
        wrapper.isNull(ApprovalRecord::getAction);
        List<ApprovalRecord> pendingRecords = recordManager.list(wrapper);
        for (ApprovalRecord r : pendingRecords) {
            recordManager.removeById(r.getId());
        }

        // 更新实例状态
        instance.setStatus("CANCELLED");
        instance.setUpdateBy(loginUser.getUsername());
        instance.setUpdateByName(loginUser.getRealName());
        instance.setUpdateTime(new Date());
        instanceManager.updateById(instance);

        // 通知业务模块恢复为草稿
        notifyBusinessStatus(instance, "DRAFT");
        log.info("审批撤回，实例ID: {}，申请人: {}", instanceId, loginUser.getUsername());
    }

    // =========================================================
    // 查询接口
    // =========================================================

    /**
     * 获取我的待审批列表
     */
    public List<ApprovalInstanceResp> getMyPending(Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<ApprovalRecord> recordWrapper = Wrappers.lambdaQuery();
        recordWrapper.eq(ApprovalRecord::getTenantId, tenantId);
        recordWrapper.eq(ApprovalRecord::getApprover, loginUser.getUsername());
        recordWrapper.isNull(ApprovalRecord::getAction);
        recordWrapper.eq(ApprovalRecord::getIsCc, 0);
        List<ApprovalRecord> pendingRecords = recordManager.list(recordWrapper);

        if (pendingRecords.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> instanceIds = pendingRecords.stream()
                .map(ApprovalRecord::getInstanceId)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<ApprovalInstance> instanceWrapper = Wrappers.lambdaQuery();
        instanceWrapper.in(ApprovalInstance::getId, instanceIds);
        instanceWrapper.eq(ApprovalInstance::getStatus, "PENDING");
        instanceWrapper.orderByDesc(ApprovalInstance::getCreateTime);
        List<ApprovalInstance> instances = instanceManager.list(instanceWrapper);

        return instances.stream().map(this::convertToResp).collect(Collectors.toList());
    }

    /**
     * 获取我提交的审批列表
     */
    public List<ApprovalInstanceResp> getMySubmitted(Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<ApprovalInstance> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApprovalInstance::getTenantId, tenantId);
        wrapper.eq(ApprovalInstance::getApplicant, loginUser.getUsername());
        wrapper.orderByDesc(ApprovalInstance::getCreateTime);
        List<ApprovalInstance> instances = instanceManager.list(wrapper);

        return instances.stream().map(this::convertToResp).collect(Collectors.toList());
    }

    /**
     * 获取审批实例详情
     */
    public ApprovalInstanceResp getInstanceDetail(Long instanceId) {
        ApprovalInstance instance = instanceManager.getById(instanceId);
        if (instance == null) {
            throw new BusinessException("审批实例不存在");
        }
        return convertToRespWithRecords(instance);
    }

    /**
     * 根据业务类型和业务ID查找审批实例
     */
    public ApprovalInstance getInstanceByBusiness(String businessType, Long businessId, Long tenantId) {
        LambdaQueryWrapper<ApprovalInstance> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApprovalInstance::getTenantId, tenantId);
        wrapper.eq(ApprovalInstance::getBusinessType, businessType);
        wrapper.eq(ApprovalInstance::getBusinessId, businessId);
        wrapper.last("LIMIT 1");
        return instanceManager.getOne(wrapper);
    }

    // =========================================================
    // 内部辅助方法
    // =========================================================

    /**
     * 推进到下一个有效节点（满足条件的节点）
     */
    private String advanceToNextNode(ApprovalInstance instance, Long tenantId,
                                      String operatorUsername, String operatorRealName) {
        ApprovalDefinition definition = definitionManager.getById(instance.getDefinitionId());
        List<ApprovalNodeConfig> nodes = parseAndSortNodes(definition.getNodesJson());

        // 找当前节点序号之后的第一个满足条件的节点
        int currentSort = instance.getCurrentNode();
        ApprovalNodeConfig nextNode = nodes.stream()
                .filter(n -> n.getSort() > currentSort)
                .filter(n -> conditionMatch(n, instance.getBusinessType(), instance.getBusinessId()))
                .findFirst()
                .orElse(null);

        if (nextNode != null) {
            instance.setCurrentNode(nextNode.getSort());
            instance.setUpdateBy(operatorUsername);
            instance.setUpdateByName(operatorRealName);
            instance.setUpdateTime(new Date());
            instanceManager.updateById(instance);

            // 使用临时LoginUser对象只携带用户名，避免依赖完整LoginUser
            createNodeRecords(instance, nextNode, tenantId, operatorUsername, operatorRealName);
            log.info("审批推进到节点 {}（{}），实例ID: {}", nextNode.getSort(), nextNode.getName(), instance.getId());
            return "PENDING";
        } else {
            // 所有节点完成，审批通过
            instance.setStatus("APPROVED");
            instance.setUpdateBy(operatorUsername);
            instance.setUpdateByName(operatorRealName);
            instance.setUpdateTime(new Date());
            instanceManager.updateById(instance);
            notifyBusinessStatus(instance, "APPROVED");
            log.info("审批全部通过，实例ID: {}", instance.getId());
            return "APPROVED";
        }
    }

    /**
     * 为节点创建审批记录（会签会创建多条）及抄送记录
     */
    private void createNodeRecords(ApprovalInstance instance, ApprovalNodeConfig node,
                                    Long tenantId, String operatorUsername, String operatorRealName) {
        List<String> approvers = resolveApprovers(node, instance.getApplicant(), tenantId);
        String signType = node.getSignType() != null ? node.getSignType() : "AND";

        for (String approver : approvers) {
            // 检查是否存在有效的审批委托
            String actualApprover = checkDelegation(approver, instance.getBusinessType());
            if (actualApprover == null) {
                actualApprover = approver;
            } else {
                log.info("审批委托生效：{} → {}，实例ID: {}", approver, actualApprover, instance.getId());
            }

            ApprovalRecord record = new ApprovalRecord();
            record.setTenantId(tenantId);
            record.setInstanceId(instance.getId());
            record.setNodeSort(node.getSort());
            record.setNodeName(node.getName());
            record.setApprover(actualApprover);
            record.setApproverName(actualApprover);
            record.setSignType(signType);
            record.setIsCc(0);
            record.setCreateBy(operatorUsername);
            record.setCreateByName(operatorRealName);
            record.setCreateTime(new Date());
            recordManager.save(record);
        }

        // 创建抄送记录
        if (node.getCcUsers() != null) {
            for (String ccUser : node.getCcUsers()) {
                OaApprovalCC cc = new OaApprovalCC();
                cc.setTenantId(tenantId);
                cc.setInstanceId(instance.getId());
                cc.setCcUser(ccUser);
                cc.setCcUserName(ccUser);
                cc.setCreateTime(new Date());
                ccManager.save(cc);
            }
        }
    }

    /**
     * 作废同节点的其他待处理记录（OR签通过或拒绝时调用）
     */
    private void voidOtherPendingRecords(Long instanceId, Integer nodeSort, Long excludeRecordId) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApprovalRecord::getInstanceId, instanceId);
        wrapper.eq(ApprovalRecord::getNodeSort, nodeSort);
        wrapper.isNull(ApprovalRecord::getAction);
        wrapper.ne(ApprovalRecord::getId, excludeRecordId);
        List<ApprovalRecord> others = recordManager.list(wrapper);
        for (ApprovalRecord r : others) {
            recordManager.removeById(r.getId());
        }
    }

    /**
     * 检查当前节点所有审批人是否全部通过（AND签）
     */
    private boolean checkAllApproved(Long instanceId, Integer nodeSort) {
        LambdaQueryWrapper<ApprovalRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApprovalRecord::getInstanceId, instanceId);
        wrapper.eq(ApprovalRecord::getNodeSort, nodeSort);
        wrapper.eq(ApprovalRecord::getIsCc, 0);
        List<ApprovalRecord> records = recordManager.list(wrapper);

        if (records.isEmpty()) {
            return false;
        }
        // 所有记录都必须 action=APPROVE
        return records.stream().allMatch(r -> "APPROVE".equals(r.getAction()));
    }

    /**
     * 检查节点条件是否满足
     */
    private boolean conditionMatch(ApprovalNodeConfig node, String businessType, Long businessId) {
        List<ApprovalNodeConfig.ApprovalCondition> conditions = node.getConditions();
        if (conditions == null || conditions.isEmpty()) {
            return true; // 无条件，始终执行
        }

        Map<String, BigDecimal> dataMap = getBusinessDataMap(businessType, businessId);

        for (ApprovalNodeConfig.ApprovalCondition cond : conditions) {
            BigDecimal fieldValue = dataMap.get(cond.getField());
            if (fieldValue == null) {
                return false; // 字段不存在则条件不满足
            }
            BigDecimal threshold;
            try {
                threshold = new BigDecimal(cond.getValue());
            } catch (NumberFormatException e) {
                return false;
            }
            if (!evaluateCondition(fieldValue, cond.getOperator(), threshold)) {
                return false; // 任一条件不满足则整个节点跳过
            }
        }
        return true;
    }

    /**
     * 条件计算
     */
    private boolean evaluateCondition(BigDecimal value, String operator, BigDecimal threshold) {
        switch (operator) {
            case "GT":  return value.compareTo(threshold) > 0;
            case "LT":  return value.compareTo(threshold) < 0;
            case "GTE": return value.compareTo(threshold) >= 0;
            case "LTE": return value.compareTo(threshold) <= 0;
            case "EQ":  return value.compareTo(threshold) == 0;
            default:    return false;
        }
    }

    /**
     * 获取业务数据用于条件评估（key: amount / duration）
     */
    private Map<String, BigDecimal> getBusinessDataMap(String businessType, Long businessId) {
        Map<String, BigDecimal> data = new HashMap<>();
        try {
            if ("EXPENSE".equals(businessType)) {
                OaExpense expense = expenseManager.getById(businessId);
                if (expense != null && expense.getTotalAmount() != null) {
                    data.put("amount", expense.getTotalAmount());
                }
            } else if ("PAYMENT".equals(businessType)) {
                OaPayment payment = paymentManager.getById(businessId);
                if (payment != null && payment.getAmount() != null) {
                    data.put("amount", payment.getAmount());
                }
            } else if ("LEAVE".equals(businessType)) {
                OaLeave leave = leaveManager.getById(businessId);
                if (leave != null && leave.getDuration() != null) {
                    data.put("duration", leave.getDuration());
                }
            } else if ("TRIP".equals(businessType)) {
                OaBusinessTrip trip = businessTripManager.getById(businessId);
                if (trip != null && trip.getEstimatedCost() != null) {
                    data.put("amount", trip.getEstimatedCost());
                }
            }
        } catch (Exception e) {
            log.warn("获取业务数据失败: businessType={}, businessId={}", businessType, businessId, e);
        }
        return data;
    }

    /**
     * 找第一个条件满足的节点
     */
    private ApprovalNodeConfig findFirstMatchingNode(List<ApprovalNodeConfig> sortedNodes,
                                                      String businessType, Long businessId) {
        return sortedNodes.stream()
                .filter(n -> conditionMatch(n, businessType, businessId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析并排序节点
     */
    private List<ApprovalNodeConfig> parseAndSortNodes(String nodesJson) {
        List<ApprovalNodeConfig> nodes = JSON.parseArray(nodesJson, ApprovalNodeConfig.class);
        if (nodes == null) {
            return new ArrayList<>();
        }
        nodes.sort(Comparator.comparingInt(n -> (n.getSort() == null ? 0 : n.getSort())));
        return nodes;
    }

    /**
     * 解析审批人列表（支持多人）
     */
    public List<String> resolveApprovers(ApprovalNodeConfig node, String applicantUsername, Long tenantId) {
        String approverType = node.getApproverType();
        if (approverType == null) {
            throw new BusinessException("审批人类型未配置");
        }
        switch (approverType) {
            case "LEADER":
                return Collections.singletonList(resolveDeptLeader(applicantUsername, tenantId));
            case "ROLE":
            case "USER":
                if (node.getApproverIds() != null && !node.getApproverIds().isEmpty()) {
                    return node.getApproverIds();
                }
                throw new BusinessException("审批人未配置");
            default:
                throw new BusinessException("不支持的审批人类型：" + approverType);
        }
    }

    /**
     * 解析审批人（兼容旧接口，取第一个）
     */
    public String resolveApprover(ApprovalNodeConfig node, String applicantUsername, Long tenantId) {
        List<String> approvers = resolveApprovers(node, applicantUsername, tenantId);
        return approvers.isEmpty() ? applicantUsername : approvers.get(0);
    }

    /**
     * 查找用户的部门负责人
     */
    private String resolveDeptLeader(String username, Long tenantId) {
        LambdaQueryWrapper<UserDepartment> udWrapper = Wrappers.lambdaQuery();
        udWrapper.eq(UserDepartment::getTenantId, tenantId);
        List<UserDepartment> userDepts = userDepartmentManager.list(udWrapper);

        if (userDepts != null && !userDepts.isEmpty()) {
            for (UserDepartment ud : userDepts) {
                Department dept = departmentManager.getById(ud.getDepartmentId());
                if (dept != null && dept.getLeaderUsername() != null) {
                    return dept.getLeaderUsername();
                }
            }
        }

        log.warn("未找到用户 {} 的部门负责人，将使用申请人自身", username);
        return username;
    }

    /**
     * 通知业务模块审批结果
     */
    private void notifyBusinessStatus(ApprovalInstance instance, String status) {
        String type = instance.getBusinessType();
        Long businessId = instance.getBusinessId();
        try {
            if ("EXPENSE".equals(type)) {
                expenseService.updateExpenseStatus(businessId, status);
            } else if ("PAYMENT".equals(type)) {
                paymentService.updatePaymentStatus(businessId, status);
            } else if ("LEAVE".equals(type)) {
                leaveService.updateLeaveStatus(businessId, status);
            } else if ("TRIP".equals(type)) {
                businessTripService.updateTripStatus(businessId, status);
            } else if ("MAKEUP_CLOCKIN".equals(type)) {
                makeupClockinService.updateMakeupClockinStatus(businessId, status);
            }
        } catch (Exception e) {
            log.error("审批状态回调失败，businessType: {}，businessId: {}，status: {}", type, businessId, status, e);
        }
    }

    // =========================================================
    // 委托与催办
    // =========================================================

    /**
     * 检查审批人是否有生效中的委托，返回被委托人账号，无委托返回null
     */
    private String checkDelegation(String approver, String businessType) {
        try {
            LambdaQueryWrapper<OaApprovalDelegation> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(OaApprovalDelegation::getDelegator, approver);
            wrapper.eq(OaApprovalDelegation::getEnabled, 1);
            wrapper.le(OaApprovalDelegation::getStartDate, new Date());
            wrapper.ge(OaApprovalDelegation::getEndDate, new Date());
            List<OaApprovalDelegation> delegations = delegationManager.list(wrapper);
            for (OaApprovalDelegation d : delegations) {
                if (d.getBusinessTypes() == null || d.getBusinessTypes().isEmpty()) {
                    return d.getDelegateTo();
                }
                if (d.getBusinessTypes().contains(businessType)) {
                    return d.getDelegateTo();
                }
            }
        } catch (Exception e) {
            log.warn("查询审批委托失败: approver={}", approver, e);
        }
        return null;
    }

    /**
     * 催办
     */
    public void sendReminder(Long instanceId, Long tenantId, LoginUser loginUser) {
        ApprovalInstance instance = instanceManager.getById(instanceId);
        if (instance == null) {
            throw new BusinessException("审批实例不存在");
        }
        if (!"PENDING".equals(instance.getStatus())) {
            throw new BusinessException("只有审批中的申请才能催办");
        }
        if (!loginUser.getUsername().equals(instance.getApplicant())) {
            throw new BusinessException("只有申请人才能催办");
        }

        // 查找当前节点的待审批人
        LambdaQueryWrapper<ApprovalRecord> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ApprovalRecord::getInstanceId, instanceId);
        wrapper.eq(ApprovalRecord::getNodeSort, instance.getCurrentNode());
        wrapper.isNull(ApprovalRecord::getAction);
        wrapper.eq(ApprovalRecord::getIsCc, 0);
        List<ApprovalRecord> pendingRecords = recordManager.list(wrapper);

        for (ApprovalRecord record : pendingRecords) {
            OaApprovalReminder reminder = new OaApprovalReminder();
            reminder.setTenantId(tenantId);
            reminder.setInstanceId(instanceId);
            reminder.setSender(loginUser.getUsername());
            reminder.setSenderName(loginUser.getRealName());
            reminder.setTargetApprover(record.getApprover());
            reminder.setSendTime(new Date());
            reminder.setMessage("请尽快处理审批：" + instance.getTitle());
            reminderManager.save(reminder);
        }

        // 更新催办次数和时间
        instance.setRemindCount((instance.getRemindCount() != null ? instance.getRemindCount() : 0) + 1);
        instance.setLastRemindedTime(new Date());
        instanceManager.updateById(instance);

        log.info("催办成功，实例ID: {}，催办人: {}", instanceId, loginUser.getUsername());
    }

    /**
     * 获取催办历史
     */
    public List<OaApprovalReminder> getReminderHistory(Long instanceId) {
        LambdaQueryWrapper<OaApprovalReminder> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaApprovalReminder::getInstanceId, instanceId);
        wrapper.orderByDesc(OaApprovalReminder::getSendTime);
        return reminderManager.list(wrapper);
    }

    // =========================================================
    // 对象转换
    // =========================================================

    private ApprovalInstanceResp convertToResp(ApprovalInstance instance) {
        ApprovalInstanceResp resp = new ApprovalInstanceResp();
        resp.setId(instance.getId());
        resp.setBusinessType(instance.getBusinessType());
        resp.setBusinessId(instance.getBusinessId());
        resp.setTitle(instance.getTitle());
        resp.setApplicant(instance.getApplicant());
        resp.setApplicantName(instance.getApplicantName());
        resp.setStatus(instance.getStatus());
        resp.setCurrentNode(instance.getCurrentNode());
        resp.setCreateTime(instance.getCreateTime());
        return resp;
    }

    private ApprovalInstanceResp convertToRespWithRecords(ApprovalInstance instance) {
        ApprovalInstanceResp resp = convertToResp(instance);

        LambdaQueryWrapper<ApprovalRecord> recordWrapper = Wrappers.lambdaQuery();
        recordWrapper.eq(ApprovalRecord::getInstanceId, instance.getId());
        recordWrapper.orderByAsc(ApprovalRecord::getNodeSort);
        List<ApprovalRecord> records = recordManager.list(recordWrapper);
        resp.setRecords(records);

        return resp;
    }
}
