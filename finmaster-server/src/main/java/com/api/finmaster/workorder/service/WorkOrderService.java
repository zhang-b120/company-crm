package com.qykj.finmaster.workorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.workorder.constants.WorkOrderAction;
import com.qykj.finmaster.workorder.constants.WorkOrderSourceType;
import com.qykj.finmaster.workorder.constants.WorkOrderStatus;
import com.qykj.finmaster.workorder.constants.WorkOrderStepCode;
import com.qykj.finmaster.workorder.constants.WorkOrderStepStatus;
import com.qykj.finmaster.workorder.constants.WorkOrderTaskType;
import com.qykj.finmaster.workorder.domain.WorkOrder;
import com.qykj.finmaster.workorder.domain.WorkOrderAttachment;
import com.qykj.finmaster.workorder.domain.WorkOrderLog;
import com.qykj.finmaster.workorder.domain.WorkOrderStep;
import com.qykj.finmaster.workorder.domain.req.CancelWorkOrderReq;
import com.qykj.finmaster.workorder.domain.req.CreateWorkOrderReq;
import com.qykj.finmaster.workorder.domain.req.ExecuteStepReq;
import com.qykj.finmaster.workorder.domain.req.QueryWorkOrderReq;
import com.qykj.finmaster.workorder.domain.req.RejectWorkOrderReq;
import com.qykj.finmaster.workorder.domain.req.SplitSubTaskReq;
import com.qykj.finmaster.workorder.domain.req.SuspendWorkOrderReq;
import com.qykj.finmaster.workorder.domain.req.UrgeWorkOrderReq;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.manager.ContractManager;
import com.qykj.finmaster.customer.company.domain.CompanyService;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.domain.req.AddCompanyRenewReq;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.customer.company.service.CompanyServiceServiceImpl;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceBusiness;
import com.qykj.finmaster.system.serviceMaintenance.manager.impl.ServiceBusinessManagerImpl;
import com.qykj.finmaster.workorder.manager.WorkOrderAttachmentManager;
import com.qykj.finmaster.workorder.manager.WorkOrderLogManager;
import com.qykj.finmaster.workorder.manager.WorkOrderManager;
import com.qykj.finmaster.workorder.manager.WorkOrderStepManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 任务单核心编排 Service
 * 状态机、节点流转、日志记录、子任务管理都在这里
 */
@Slf4j
@Service
public class WorkOrderService {

    @Autowired
    private WorkOrderManager workOrderManager;

    @Autowired
    private WorkOrderStepManager stepManager;

    @Autowired
    private WorkOrderLogManager logManager;

    @Autowired
    private WorkOrderAttachmentManager attachmentManager;

    @Autowired
    private ContractManager contractManager;

    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;

    @Autowired
    private com.qykj.finmaster.order.manager.OrderDetailManager orderDetailManager;

    @Autowired
    private CustomerInfoManagerImpl customerInfoManager;

    @Autowired
    private com.qykj.finmaster.order.manager.RefundOrderInfoManager refundOrderInfoManager;
    @Autowired
    private com.qykj.finmaster.order.manager.ServiceTransferItemManager serviceTransferItemManager;

    @Autowired
    private com.qykj.finmaster.customer.company.manager.impl.CompanyServiceManagerImpl companyServiceManager;

    @Autowired
    private ServiceBusinessManagerImpl serviceBusinessManager;

    @org.springframework.context.annotation.Lazy
    @Autowired
    private CompanyServiceServiceImpl companyServiceService;

    @Autowired
    private WorkOrderBookkeepingHelper bookkeepingHelper;

    @Autowired
    private BookkeepingDispatchHelper bookkeepingDispatchHelper;

    @Autowired
    private WorkOrderNotificationService notificationService;

    /* ==================== 创建 ==================== */

    /**
     * 创建顶级任务：生成主单 + 节点列表 + 初始日志
     * 初始状态 PENDING；第一个节点 is_current=1；当前持有人 = 指派的第一节点负责人
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder createWorkOrder(CreateWorkOrderReq req, LoginUser loginUser, Long tenantId) {
        WorkOrderTaskType type = parseType(req.getTaskType());
        if (!type.canBeTopLevel()) {
            throw new BusinessException(type.name() + " 只能作为子任务创建，不能独立发起");
        }
        List<WorkOrderStepCode> stepCodes = WorkOrderStepCode.defaultStepsFor(type);
        if (stepCodes.isEmpty()) {
            throw new BusinessException("任务类型未定义节点");
        }

        // 代账任务：先保存公司/股东/员工/税务凭证 4 块数据（事务内），再创建任务单
        if (type == WorkOrderTaskType.BOOKKEEPING) {
            if (req.getBookkeepingPayload() == null) {
                throw new BusinessException("代账派工必须填写公司/股东/员工信息");
            }
            if (req.getCompanyId() == null) {
                throw new BusinessException("代账派工必须关联公司");
            }
            bookkeepingDispatchHelper.saveAll(req.getCompanyId(), req.getBookkeepingPayload(), loginUser, tenantId);
        }

        Date now = new Date();
        WorkOrder wo = new WorkOrder();
        wo.setTenantId(tenantId);
        wo.setTaskNo(generateTaskNo());
        wo.setTaskType(type.name());
        wo.setSubType(req.getSubType());
        wo.setSourceType(StringUtils.defaultString(req.getSourceType(), WorkOrderSourceType.MANUAL.name()));
        wo.setSourceId(req.getSourceId());
        wo.setSourceNumber(req.getSourceNumber());
        wo.setContractId(req.getContractId());
        wo.setOrderId(req.getOrderId());
        wo.setCompanyId(req.getCompanyId());
        wo.setCustomerId(req.getCustomerId());
        wo.setStatus(WorkOrderStatus.PENDING.name());
        wo.setTitle(req.getTitle());
        wo.setDescription(req.getDescription());
        wo.setRemark(req.getRemark());
        wo.setMetaJson(req.getMetaJson());
        wo.setOwnerUser(loginUser.getUsername());
        wo.setOwnerUserName(loginUser.getRealName());
        // current_owner_user = 第一节点的负责人（优先从 stepOwners 取，没有再回落 assignedOwnerUser）
        wo.setCurrentOwnerUser(resolveStepOwner(req, stepCodes.get(0), 1));
        wo.setSlaHours(req.getSlaHours());
        if (req.getSlaHours() != null && req.getSlaHours() > 0) {
            wo.setDueTime(new Date(now.getTime() + req.getSlaHours() * 3600_000L));
        }
        // 代账任务：账套启用日期默认上月 1 号；账套截止日期默认启用日期 + 1 年；管家可在详情页修改
        if (type == WorkOrderTaskType.BOOKKEEPING) {
            java.time.LocalDate enableDate = java.time.LocalDate.now()
                    .minusMonths(1)
                    .with(java.time.temporal.TemporalAdjusters.firstDayOfMonth());
            wo.setAccountSetEnableDate(enableDate);
            wo.setAccountSetExpiryDate(enableDate.plusYears(1));
        }
        wo.setCreateBy(loginUser.getUsername());
        wo.setCreateByName(loginUser.getRealName());
        wo.setCreateTime(now);
        wo.setUpdateBy(loginUser.getUsername());
        wo.setUpdateByName(loginUser.getRealName());
        wo.setUpdateTime(now);
        workOrderManager.save(wo);

        // 创建节点：每个节点按 stepOwners 查找各自负责人，找不到时只有第一节点回落到 assignedOwnerUser
        int sort = 1;
        for (WorkOrderStepCode code : stepCodes) {
            WorkOrderStep s = new WorkOrderStep();
            s.setTenantId(tenantId);
            s.setWorkOrderId(wo.getId());
            s.setStepCode(code.name());
            s.setStepName(code.getLabel());
            s.setSort(sort);
            s.setIsCurrent(sort == 1 ? 1 : 0);
            s.setStatus(WorkOrderStepStatus.PENDING.name());
            s.setCreateTime(now);
            s.setUpdateTime(now);
            String owner = resolveStepOwner(req, code, sort);
            if (StringUtils.isNotBlank(owner)) {
                s.setOwnerUser(owner);
            }
            stepManager.save(s);
            sort++;
        }

        writeLog(wo.getId(), null, WorkOrderAction.CREATE,
                null, WorkOrderStatus.PENDING.name(),
                null, wo.getCurrentOwnerUser(),
                "任务创建", loginUser, tenantId);
        // 推送给当前持有人，由前端右下角弹窗提醒处理
        notificationService.notifyAssign(wo, wo.getCurrentOwnerUser());
        return wo;
    }

    /**
     * 根据 stepOwners 查找指定 stepCode 的负责人；找不到时第 1 节点回落到 assignedOwnerUser
     */
    private String resolveStepOwner(CreateWorkOrderReq req, WorkOrderStepCode code, int sort) {
        if (req.getStepOwners() != null) {
            for (var so : req.getStepOwners()) {
                if (code.name().equalsIgnoreCase(so.getStepCode())
                        && StringUtils.isNotBlank(so.getOwnerUser())) {
                    return so.getOwnerUser();
                }
            }
        }
        if (sort == 1) return req.getAssignedOwnerUser();
        return null;
    }

    /**
     * 从父任务拆出子任务（子类型限定 FIELD_WORK / PAYMENT / REFUND / OTHER）
     * dispatch=true → 立即下发（PENDING + 推送处理人）；
     * dispatch=false → 保存为草稿（DRAFT，处理人不可见，需调 dispatchSubTask 才下发）。
     * 仅当子任务进入非草稿状态后，父任务才会切到 WAITING_CHILD（避免草稿阻塞父任务完成）。
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder splitSubTask(SplitSubTaskReq req, LoginUser loginUser, Long tenantId) {
        WorkOrder parent = getAndCheck(req.getParentTaskId());
        assertNotWaitingFix(parent);
        if (WorkOrderStatus.DONE.name().equals(parent.getStatus())
                || WorkOrderStatus.CANCELED.name().equals(parent.getStatus())) {
            throw new BusinessException("已结束的任务不能再拆分子任务");
        }
        WorkOrderTaskType subType = parseType(req.getTaskType());
        if (!subType.canBeSubTask()) {
            throw new BusinessException(subType.name() + " 不能作为子任务");
        }
        boolean dispatch = !Boolean.FALSE.equals(req.getDispatch());

        CreateWorkOrderReq createReq = new CreateWorkOrderReq();
        createReq.setTaskType(req.getTaskType());
        createReq.setSourceType(WorkOrderSourceType.PARENT.name());
        createReq.setSourceId(parent.getId());
        createReq.setContractId(parent.getContractId());
        createReq.setOrderId(parent.getOrderId());
        createReq.setCompanyId(parent.getCompanyId());
        createReq.setCustomerId(parent.getCustomerId());
        createReq.setTitle(req.getTitle());
        createReq.setDescription(req.getDescription());
        createReq.setMetaJson(req.getMetaJson());
        createReq.setRemark(req.getRemark());
        createReq.setAssignedOwnerUser(req.getAssignedOwnerUser());

        WorkOrder child = createWorkOrder(createReq, loginUser, tenantId);
        child.setParentTaskId(parent.getId());
        if (!dispatch) {
            // 草稿：把子任务和它的所有节点回退为 DRAFT，处理人在自己的列表里看不到
            child.setStatus(WorkOrderStatus.DRAFT.name());
            stepManager.lambdaUpdate()
                    .eq(WorkOrderStep::getWorkOrderId, child.getId())
                    .set(WorkOrderStep::getStatus, WorkOrderStepStatus.PENDING.name())
                    .update();
        }
        workOrderManager.updateById(child);

        if (dispatch) {
            String prevStatus = parent.getStatus();
            parent.setStatus(WorkOrderStatus.WAITING_CHILD.name());
            parent.setUpdateTime(new Date());
            workOrderManager.updateById(parent);
            writeLog(parent.getId(), null, WorkOrderAction.SPLIT,
                    prevStatus, WorkOrderStatus.WAITING_CHILD.name(),
                    null, null,
                    "拆出子任务：" + child.getTaskNo(), loginUser, tenantId);
            notificationService.notifyAssign(child, req.getAssignedOwnerUser());
        } else {
            writeLog(parent.getId(), null, WorkOrderAction.SPLIT,
                    parent.getStatus(), parent.getStatus(),
                    null, null,
                    "新建草稿子任务：" + child.getTaskNo() + "（待下发）", loginUser, tenantId);
        }
        return child;
    }

    /**
     * 下发草稿子任务：DRAFT → PENDING + 推送处理人 + 把父任务切到 WAITING_CHILD。
     * 仅父任务持有人或子任务发起人可下发。
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder dispatchSubTask(Long subTaskId, LoginUser loginUser) {
        WorkOrder child = getAndCheck(subTaskId);
        if (!WorkOrderStatus.DRAFT.name().equals(child.getStatus())) {
            throw new BusinessException("只有草稿子任务可以下发");
        }
        if (child.getParentTaskId() == null) {
            throw new BusinessException("不是子任务，无法下发");
        }
        if (StringUtils.isBlank(child.getCurrentOwnerUser())) {
            throw new BusinessException("未指派处理人，请先编辑后再下发");
        }
        Date now = new Date();
        child.setStatus(WorkOrderStatus.PENDING.name());
        child.setUpdateTime(now);
        workOrderManager.updateById(child);

        WorkOrder parent = workOrderManager.getById(child.getParentTaskId());
        if (parent != null
                && !WorkOrderStatus.DONE.name().equals(parent.getStatus())
                && !WorkOrderStatus.CANCELED.name().equals(parent.getStatus())
                && !WorkOrderStatus.WAITING_CHILD.name().equals(parent.getStatus())) {
            String prev = parent.getStatus();
            parent.setStatus(WorkOrderStatus.WAITING_CHILD.name());
            parent.setUpdateTime(now);
            workOrderManager.updateById(parent);
            writeLog(parent.getId(), null, WorkOrderAction.SPLIT,
                    prev, WorkOrderStatus.WAITING_CHILD.name(),
                    null, null,
                    "下发子任务：" + child.getTaskNo(), loginUser, parent.getTenantId());
        }
        writeLog(child.getId(), null, WorkOrderAction.ASSIGN,
                WorkOrderStatus.DRAFT.name(), WorkOrderStatus.PENDING.name(),
                null, child.getCurrentOwnerUser(),
                "草稿下发", loginUser, child.getTenantId());
        notificationService.notifyAssign(child, child.getCurrentOwnerUser());
        return child;
    }

    /**
     * 修改子任务（仅 DRAFT 状态可改）：标题/描述/备注/metaJson/处理人
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder updateSubTaskDraft(SplitSubTaskReq req, LoginUser loginUser) {
        WorkOrder child = getAndCheck(req.getParentTaskId()); // 复用 parentTaskId 字段承载子任务 id
        if (!WorkOrderStatus.DRAFT.name().equals(child.getStatus())) {
            throw new BusinessException("仅草稿子任务可修改");
        }
        Date now = new Date();
        if (StringUtils.isNotBlank(req.getTitle())) child.setTitle(req.getTitle());
        if (req.getDescription() != null) child.setDescription(req.getDescription());
        if (req.getRemark() != null) child.setRemark(req.getRemark());
        if (req.getMetaJson() != null) child.setMetaJson(req.getMetaJson());
        if (StringUtils.isNotBlank(req.getAssignedOwnerUser())) {
            child.setCurrentOwnerUser(req.getAssignedOwnerUser());
            // 同步第一节点的 owner
            WorkOrderStep first = stepManager.lambdaQuery()
                    .eq(WorkOrderStep::getWorkOrderId, child.getId())
                    .orderByAsc(WorkOrderStep::getSort)
                    .last("limit 1")
                    .one();
            if (first != null) {
                first.setOwnerUser(req.getAssignedOwnerUser());
                first.setUpdateTime(now);
                stepManager.updateById(first);
            }
        }
        child.setUpdateTime(now);
        workOrderManager.updateById(child);
        writeLog(child.getId(), null, WorkOrderAction.COMMENT,
                WorkOrderStatus.DRAFT.name(), WorkOrderStatus.DRAFT.name(),
                null, null,
                "草稿子任务被修改", loginUser, child.getTenantId());
        return child;
    }

    /* ==================== 执行 ==================== */

    /**
     * 节点负责人点"完成"：当前节点标 DONE
     * - 有下一节点 → 下一节点 PENDING→RUNNING，主任务 current_owner_user 切换
     * - 没有下一节点 → 检查是否有未完成子任务，有则等待，否则主任务 DONE
     */
    @Transactional(rollbackFor = Exception.class)
    public WorkOrder executeCurrentStep(ExecuteStepReq req, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(req.getWorkOrderId());
        assertNotWaitingFix(wo);
        if (WorkOrderStatus.DONE.name().equals(wo.getStatus())
                || WorkOrderStatus.CANCELED.name().equals(wo.getStatus())) {
            throw new BusinessException("任务已结束，无法执行");
        }
        WorkOrderStep current = getCurrentStep(wo.getId());
        if (current == null) throw new BusinessException("找不到当前节点");
        if (!Objects.equals(current.getOwnerUser(), loginUser.getUsername())) {
            throw new BusinessException("只有节点负责人可以完成该节点");
        }
        // 若主任务处在 WAITING_CHILD，必须所有子任务都 DONE 才能完成父节点
        if (WorkOrderStatus.WAITING_CHILD.name().equals(wo.getStatus())) {
            if (!allChildrenDone(wo.getId())) {
                throw new BusinessException("仍有子任务未完成，无法完成该节点");
            }
        }
        // 数据收集任务：所有公司必须均已上传或勾选无流水方可完成
        if (WorkOrderTaskType.DATA_COLLECTION.name().equals(wo.getTaskType())
                && WorkOrderStepCode.DATA_COLLECT_EXECUTE.name().equals(current.getStepCode())) {
            java.util.Map<String, Object> stat = listDataCollectionCompanies(wo.getId());
            int total = stat.get("total") instanceof Number ? ((Number) stat.get("total")).intValue() : 0;
            long doneCount = stat.get("doneCount") instanceof Number ? ((Number) stat.get("doneCount")).longValue() : 0L;
            if (total > 0 && doneCount < total) {
                throw new BusinessException("仍有 " + (total - doneCount) + " 家公司未完成银行流水收集，无法完成任务");
            }
        }

        Date now = new Date();
        current.setFormDataJson(req.getFormDataJson());
        current.setStatus(WorkOrderStepStatus.DONE.name());
        current.setIsCurrent(0);
        current.setCompleteTime(now);
        current.setUpdateTime(now);
        stepManager.updateById(current);

        // 让步完成：必须填写让步原因 + 计划处理时间；写入工单字段，便于财务后期跟进对接
        if (req.getConcessionComplete() != null && req.getConcessionComplete() == 1) {
            if (StringUtils.isBlank(req.getConcessionReason())) {
                throw new BusinessException("让步完成必须填写让步原因 / 缺失项说明");
            }
            if (req.getConcessionPlannedTime() == null) {
                throw new BusinessException("让步完成必须填写计划处理时间");
            }
            wo.setConcessionComplete(1);
            wo.setConcessionReason(req.getConcessionReason());
            wo.setConcessionPlannedTime(req.getConcessionPlannedTime());
            // 让步项编码列表 → JSON 字符串持久化（前端 5 类编码）
            if (req.getConcessionItems() != null && !req.getConcessionItems().isEmpty()) {
                wo.setConcessionItems(com.alibaba.fastjson.JSON.toJSONString(req.getConcessionItems()));
            }
            wo.setConcessionTime(now);
            wo.setConcessionUser(loginUser != null ? loginUser.getUsername() : null);
            wo.setConcessionResolved(0);
            // 在节点日志里追加一条让步记录
            writeLog(wo.getId(), current.getId(), WorkOrderAction.EXECUTE,
                    wo.getStatus(), wo.getStatus(),
                    null, null,
                    "【让步完成】" + req.getConcessionReason(), loginUser, wo.getTenantId());
            // 让步完成本身不直接调财务系统，由下游闭环：
            //   1) 会计完成 FINANCE_RECEIVE 节点时 → onFinanceReceived 走完整 4 步（创建账套 / 迁移 / 员工 / 更新）
            //   2) 客服后期 resolveConcession 补齐 → syncOnConcessionResolved 增量更新账套 + 员工
            log.info("[让步完成] workOrderId={}, companyId={}, items={}, plannedTime={}",
                    wo.getId(), wo.getCompanyId(), req.getConcessionItems(), req.getConcessionPlannedTime());
        }

        // 节点业务钩子：代账-财务收到 → 自动建账套 + 数据迁移
        triggerStepPostActions(wo, current, loginUser);

        WorkOrderStep next = getNextStep(wo.getId(), current.getSort());
        String prevStatus = wo.getStatus();

        // === 代账-管家完成 HANDYMAN_FILL → 老任务结束 + 派生/复用"财务跟进"任务 ===
        // 首次提交：新建 FINANCE_FOLLOW_UP 工单转给会计
        // 二次提交（曾被会计驳回）：复用已有的 FINANCE_FOLLOW_UP（WAITING_FIX → PENDING），同步最新数据
        if (WorkOrderTaskType.BOOKKEEPING.name().equals(wo.getTaskType())
                && WorkOrderStepCode.HANDYMAN_FILL.name().equals(current.getStepCode())) {
            // IDV-08：完成时硬核验拦截——按公司取库里法人/股东/员工实名核验，不一致抛异常整体回滚、工单留 RUNNING
            bookkeepingDispatchHelper.validateIdCardsByCompany(wo.getCompanyId());

            // 检查是否已派生过财务跟进单（即驳回后的二次提交场景）
            WorkOrder existingFollowUp = findFinanceFollowUp(wo.getId());
            boolean isResubmit = existingFollowUp != null;

            // 解析会计 owner：二次提交沿用上次会计；首次提交按 req / 省份兜底
            String financeOwner;
            if (isResubmit) {
                if (!WorkOrderStatus.WAITING_FIX.name().equals(existingFollowUp.getStatus())) {
                    throw new BusinessException("源工单已有财务跟进单 " + existingFollowUp.getTaskNo()
                            + "，但状态非 WAITING_FIX（实际 " + existingFollowUp.getStatus() + "），请联系管理员");
                }
                financeOwner = findLastFinanceReceiverOwner(existingFollowUp.getId());
                if (StringUtils.isBlank(financeOwner)) {
                    throw new BusinessException("无法确定上次会计，请联系管理员");
                }
            } else {
                financeOwner = req.getNextStepOwner();
                if (StringUtils.isBlank(financeOwner)) {
                    String[] auto = bookkeepingHelper.resolveFinanceOwnerByCompany(wo.getCompanyId());
                    if (auto != null) financeOwner = auto[0];
                }
                if (StringUtils.isBlank(financeOwner)) {
                    throw new BusinessException("未指派会计且无法自动分配，请在弹窗中选择会计后再提交");
                }
            }

            // 1) 老工单结束 / 反激活后的"再次完成"也走相同路径（DONE 或 CONCESSION_DONE）
            finalizeOldWorkOrderAfterHandyman(wo, current, next, prevStatus, req, loginUser, now);

            // 2) 派生或复用财务跟进单
            if (isResubmit) {
                reactivateFollowUp(wo, existingFollowUp, financeOwner, loginUser, now);
            } else {
                createFinanceFollowUp(wo, financeOwner, loginUser, now);
            }

            // 3) 父任务联动（无父任务则跳过）
            notifyParentIfAllChildDone(wo, loginUser);
            return wo;
        }

        if (next != null) {
            // === 代账-客服团队长审核完成 → 必须指派管家给下一节点 HANDYMAN_FILL ===
            if (WorkOrderTaskType.BOOKKEEPING.name().equals(wo.getTaskType())
                    && WorkOrderStepCode.CS_LEAD_REVIEW.name().equals(current.getStepCode())) {
                if (StringUtils.isBlank(req.getNextStepOwner())) {
                    throw new BusinessException("审核通过时请选择具体管家");
                }
            }

            // === 其它任务的常规节点流转 ===
            // 优先使用 req.nextStepOwner（前端在完成弹窗里显式指派的下一节点负责人）
            if (StringUtils.isNotBlank(req.getNextStepOwner())) {
                next.setOwnerUser(req.getNextStepOwner());
                next.setOwnerUserName(null);
                log.info("[节点流转] 由提交方显式指派下一节点持有人 workOrderId={}, owner={}", wo.getId(), req.getNextStepOwner());
            }

            next.setIsCurrent(1);
            next.setStatus(WorkOrderStepStatus.PENDING.name());
            next.setStartTime(now);
            next.setUpdateTime(now);
            stepManager.updateById(next);

            wo.setStatus(WorkOrderStatus.RUNNING.name());
            // 若下一节点负责人未设置，则回退给发起人暂时持有，由发起人去分派
            wo.setCurrentOwnerUser(
                    StringUtils.isNotBlank(next.getOwnerUser()) ? next.getOwnerUser() : wo.getOwnerUser());
            wo.setUpdateTime(now);
            workOrderManager.updateById(wo);

            writeLog(wo.getId(), current.getId(), WorkOrderAction.EXECUTE,
                    prevStatus, WorkOrderStatus.RUNNING.name(),
                    current.getOwnerUser(), next.getOwnerUser(),
                    StringUtils.defaultString(req.getRemark(), "节点完成，流转到下一节点"),
                    loginUser, wo.getTenantId());
            // 推送给下一节点负责人
            notificationService.notifyAssign(wo, next.getOwnerUser());
        } else {
            // 末节点：整单完成。让步完成时置 CONCESSION_DONE，否则置 DONE。
            // 任务完成后，currentOwnerUser 保持为完成最后一节点的处理人，便于工作量统计 / 报表归档。
            boolean isConcessionFinal = req.getConcessionComplete() != null && req.getConcessionComplete() == 1;
            String finalEndStatus = isConcessionFinal ? WorkOrderStatus.CONCESSION_DONE.name() : WorkOrderStatus.DONE.name();
            wo.setStatus(finalEndStatus);
            String finalProcessor = StringUtils.defaultIfBlank(current.getOwnerUser(), wo.getCurrentOwnerUser());
            wo.setCurrentOwnerUser(finalProcessor);
            wo.setCompleteTime(now);
            wo.setUpdateTime(now);
            workOrderManager.updateById(wo);

            writeLog(wo.getId(), current.getId(), WorkOrderAction.COMPLETE,
                    prevStatus, finalEndStatus,
                    current.getOwnerUser(), null,
                    StringUtils.defaultString(req.getRemark(), isConcessionFinal ? "让步完成" : "任务完成"),
                    loginUser, wo.getTenantId());
            // 推送任务完成通知到发起人（左下角弹窗）
            notificationService.notifyComplete(wo, loginUser != null ? loginUser.getRealName() : null);

            // 子任务完成后，联动检查父任务（此时父可能处 WAITING_CHILD，若所有子都完成可以解锁父节点完成条件）
            notifyParentIfAllChildDone(wo, loginUser);
        }
        return wo;
    }

    /* ==================== 驳回 ==================== */

    @Transactional(rollbackFor = Exception.class)
    public void rejectWorkOrder(RejectWorkOrderReq req, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(req.getWorkOrderId());
        assertNotWaitingFix(wo);
        WorkOrderStep current = getCurrentStep(wo.getId());
        if (current == null) throw new BusinessException("找不到当前节点");
        if (!Objects.equals(current.getOwnerUser(), loginUser.getUsername())) {
            throw new BusinessException("只有节点负责人可驳回");
        }

        Date now = new Date();

        // === FINANCE_FOLLOW_UP 财务跟进单的 FINANCE_RECEIVE 节点驳回特殊处理：===
        // 不进 REJECTED 终态，而是把财务跟进单转为 WAITING_FIX，把源工单（管家工单）反激活回 RUNNING，
        // 让管家在自己的工单上修正后再次提交。详见 handleFinanceFollowUpReject。
        boolean isFinanceFollowUpReceive =
                "FINANCE_FOLLOW_UP".equals(wo.getSubType())
                && WorkOrderStepCode.FINANCE_RECEIVE.name().equals(current.getStepCode());
        if (isFinanceFollowUpReceive) {
            handleFinanceFollowUpReject(wo, current, req, loginUser, now);
            return;
        }

        current.setStatus(WorkOrderStepStatus.REJECTED.name());
        current.setRejectReason(req.getReason());
        current.setIsCurrent(0);
        current.setUpdateTime(now);
        stepManager.updateById(current);

        String prevStatus = wo.getStatus();
        WorkOrderStep prev = getPrevStep(wo.getId(), current.getSort());
        if (prev != null) {
            // 多节点任务：退回上一节点
            prev.setIsCurrent(1);
            prev.setStatus(WorkOrderStepStatus.RUNNING.name());
            prev.setStartTime(now);
            prev.setCompleteTime(null);
            prev.setUpdateTime(now);
            stepManager.updateById(prev);

            wo.setStatus(WorkOrderStatus.RUNNING.name());
            wo.setCurrentOwnerUser(prev.getOwnerUser());
            wo.setUpdateTime(now);
            workOrderManager.updateById(wo);

            writeLog(wo.getId(), current.getId(), WorkOrderAction.REJECT,
                    prevStatus, WorkOrderStatus.RUNNING.name(),
                    current.getOwnerUser(), prev.getOwnerUser(),
                    "驳回原因：" + req.getReason(),
                    loginUser, wo.getTenantId());
        } else {
            // 首节点（无上一节点）被驳回：任务整体作废 → REJECTED 终态，不退回到发起人 PENDING
            wo.setStatus(WorkOrderStatus.REJECTED.name());
            wo.setCurrentOwnerUser(null);
            wo.setUpdateTime(now);
            workOrderManager.updateById(wo);

            writeLog(wo.getId(), current.getId(), WorkOrderAction.REJECT,
                    prevStatus, WorkOrderStatus.REJECTED.name(),
                    current.getOwnerUser(), null,
                    "驳回原因：" + req.getReason(),
                    loginUser, wo.getTenantId());

            // 退费 / 服务转移工单首节点被驳回：还原原销售订单 + 删除退款订单 / 退款信息
            // 服务转移多一步：物理删该工单挂的 service_transfer_item 记录
            if (WorkOrderTaskType.REFUND.name().equals(wo.getTaskType())
                    || WorkOrderTaskType.SERVICE_TRANSFER.name().equals(wo.getTaskType())) {
                rollbackRefundOnReject(wo);
            }
        }
    }

    /**
     * 财务跟进单（FINANCE_FOLLOW_UP）的 FINANCE_RECEIVE 节点被驳回时的特殊处理：
     *
     *  1) 当前 FINANCE_RECEIVE step 标 REJECTED + 记驳回原因，isCurrent=0 作为历史保留
     *  2) 财务跟进单状态：PENDING/RUNNING → WAITING_FIX，currentOwnerUser 清空
     *  3) 老工单（管家工单）反激活：
     *     - HANDYMAN_FILL step：DONE → RUNNING, isCurrent=1, 清 completeTime, 写 reject_reason
     *     - 老工单：DONE/CONCESSION_DONE → RUNNING, currentOwner=管家, 清 completeTime
     *  4) 双工单写日志（REJECT / REOPEN），推送通知给管家
     *
     *  注：老工单的 HANDYMAN_FILL 是原地 mutate 同一行（每次驳回-修正循环不新增 step 行）；
     *      财务跟进单的 FINANCE_RECEIVE 历史 step 行全部保留，二次提交时 sort 累加新建。
     */
    private void handleFinanceFollowUpReject(WorkOrder followUp,
                                              WorkOrderStep currentReceive,
                                              RejectWorkOrderReq req,
                                              LoginUser loginUser,
                                              Date now) {
        // 1) 当前 FINANCE_RECEIVE step → REJECTED（保留作为历史行）
        currentReceive.setStatus(WorkOrderStepStatus.REJECTED.name());
        currentReceive.setRejectReason(req.getReason());
        currentReceive.setIsCurrent(0);
        currentReceive.setCompleteTime(now);
        currentReceive.setUpdateTime(now);
        stepManager.updateById(currentReceive);

        // 2) 财务跟进单 → WAITING_FIX
        String followUpPrev = followUp.getStatus();
        followUp.setStatus(WorkOrderStatus.WAITING_FIX.name());
        followUp.setCurrentOwnerUser(null);
        followUp.setUpdateTime(now);
        workOrderManager.updateById(followUp);
        writeLog(followUp.getId(), currentReceive.getId(), WorkOrderAction.REJECT,
                followUpPrev, WorkOrderStatus.WAITING_FIX.name(),
                currentReceive.getOwnerUser(), null,
                "驳回原因：" + req.getReason() + "（已退回源工单管家修正）",
                loginUser, followUp.getTenantId());

        // 3) 找老工单（HANDYMAN_FILL 所在的 BOOKKEEPING 工单）
        Long oldWoId = followUp.getSourceId();
        if (oldWoId == null) {
            log.error("[财务跟进驳回] followUpId={} 缺少 sourceId，无法回退到管家", followUp.getId());
            throw new BusinessException("财务跟进单缺少源工单关联，无法回退给管家");
        }
        WorkOrder oldWo = workOrderManager.getById(oldWoId);
        if (oldWo == null) {
            log.error("[财务跟进驳回] 源工单已不存在 oldWoId={}", oldWoId);
            throw new BusinessException("源工单已不存在，无法回退");
        }
        if (!WorkOrderTaskType.BOOKKEEPING.name().equals(oldWo.getTaskType())) {
            log.error("[财务跟进驳回] 源工单类型不是 BOOKKEEPING：{}", oldWo.getTaskType());
            throw new BusinessException("源工单类型异常，无法回退");
        }
        WorkOrderStep oldHandyStep = stepManager.lambdaQuery()
                .eq(WorkOrderStep::getWorkOrderId, oldWo.getId())
                .eq(WorkOrderStep::getStepCode, WorkOrderStepCode.HANDYMAN_FILL.name())
                .last("LIMIT 1")
                .one();
        if (oldHandyStep == null) {
            log.error("[财务跟进驳回] 源工单缺少 HANDYMAN_FILL 节点 oldWoId={}", oldWoId);
            throw new BusinessException("源工单缺少 HANDYMAN_FILL 节点，无法回退");
        }

        // 4) 反激活老工单 + HANDYMAN_FILL 节点
        oldHandyStep.setStatus(WorkOrderStepStatus.RUNNING.name());
        oldHandyStep.setIsCurrent(1);
        oldHandyStep.setCompleteTime(null);
        oldHandyStep.setRejectReason(req.getReason());  // 复用 reject_reason 字段记录会计的驳回原因
        oldHandyStep.setUpdateTime(now);
        stepManager.updateById(oldHandyStep);

        String oldPrevStatus = oldWo.getStatus();
        oldWo.setStatus(WorkOrderStatus.RUNNING.name());
        oldWo.setCurrentOwnerUser(oldHandyStep.getOwnerUser());
        oldWo.setCompleteTime(null);
        oldWo.setUpdateTime(now);
        workOrderManager.updateById(oldWo);

        // 5) 老工单写"被会计驳回，重新打开"日志 + 推送给管家
        String reopenContent = "由会计 "
                + (loginUser != null && StringUtils.isNotBlank(loginUser.getRealName())
                        ? loginUser.getRealName() : "未知")
                + " 驳回（财务跟进单 " + followUp.getTaskNo() + "），请按驳回原因修正后再次提交。\n"
                + "驳回原因：" + req.getReason();
        writeLog(oldWo.getId(), oldHandyStep.getId(), WorkOrderAction.REOPEN,
                oldPrevStatus, WorkOrderStatus.RUNNING.name(),
                loginUser != null ? loginUser.getUsername() : null,
                oldHandyStep.getOwnerUser(),
                reopenContent, loginUser, oldWo.getTenantId());
        notificationService.notifyAssign(oldWo, oldHandyStep.getOwnerUser());
        log.info("[财务跟进驳回] followUp={} → WAITING_FIX；oldWo={} 反激活回 RUNNING，管家={}",
                followUp.getTaskNo(), oldWo.getTaskNo(), oldHandyStep.getOwnerUser());
    }

    /**
     * 老工单（管家工单）HANDYMAN_FILL 完成时的收尾：
     * 修改 step + 工单状态为 DONE / CONCESSION_DONE，写日志，推送完成通知。
     * 首次提交 + 二次提交（被反激活后再完成）都走这个相同路径。
     */
    private void finalizeOldWorkOrderAfterHandyman(WorkOrder wo, WorkOrderStep current,
                                                    WorkOrderStep next, String prevStatus,
                                                    ExecuteStepReq req, LoginUser loginUser, Date now) {
        if (next != null) {
            next.setStatus(WorkOrderStepStatus.SKIPPED.name());
            next.setUpdateTime(now);
            stepManager.updateById(next);
        }
        boolean isConcession = req.getConcessionComplete() != null && req.getConcessionComplete() == 1;
        String finalStatus = isConcession ? WorkOrderStatus.CONCESSION_DONE.name() : WorkOrderStatus.DONE.name();
        wo.setStatus(finalStatus);
        String handymanProcessor = StringUtils.defaultIfBlank(current.getOwnerUser(), wo.getCurrentOwnerUser());
        wo.setCurrentOwnerUser(handymanProcessor);
        wo.setCompleteTime(now);
        wo.setUpdateTime(now);
        workOrderManager.updateById(wo);
        writeLog(wo.getId(), current.getId(), WorkOrderAction.COMPLETE,
                prevStatus, finalStatus,
                current.getOwnerUser(), null,
                StringUtils.defaultString(req.getRemark(),
                        isConcession ? "管家让步完成，已转交会计（待客服跟进补齐让步项）" : "管家完成，已转交会计"),
                loginUser, wo.getTenantId());
        notificationService.notifyComplete(wo, loginUser != null ? loginUser.getRealName() : null);
    }

    /**
     * 首次提交路径：新建 FINANCE_FOLLOW_UP 财务跟进单 + 第一个 FINANCE_RECEIVE 节点 + 复制 OPENING_BALANCE 附件。
     * 既保留原有行为不变。
     */
    private void createFinanceFollowUp(WorkOrder wo, String financeOwner, LoginUser loginUser, Date now) {
        WorkOrder newWo = new WorkOrder();
        newWo.setTenantId(wo.getTenantId());
        newWo.setTaskNo(generateTaskNo());
        newWo.setTaskType(WorkOrderTaskType.BOOKKEEPING.name());
        newWo.setSubType("FINANCE_FOLLOW_UP");
        newWo.setSourceType(WorkOrderSourceType.PARENT.name());
        newWo.setSourceId(wo.getId());
        newWo.setSourceNumber(wo.getTaskNo());
        newWo.setContractId(wo.getContractId());
        newWo.setOrderId(wo.getOrderId());
        newWo.setCompanyId(wo.getCompanyId());
        newWo.setCustomerId(wo.getCustomerId());
        newWo.setStatus(WorkOrderStatus.PENDING.name());
        newWo.setTitle(StringUtils.defaultString(wo.getTitle(), "代理记账") + " - 财务跟进");
        newWo.setDescription(wo.getDescription());
        newWo.setMetaJson(wo.getMetaJson());
        newWo.setVerifiedSections(wo.getVerifiedSections());
        newWo.setOpeningDataType(wo.getOpeningDataType());
        newWo.setOpeningDataRemark(wo.getOpeningDataRemark());
        newWo.setAccountSetEnableDate(wo.getAccountSetEnableDate());
        newWo.setAccountSetExpiryDate(wo.getAccountSetExpiryDate());
        if (wo.getConcessionComplete() != null && wo.getConcessionComplete() == 1) {
            newWo.setConcessionComplete(1);
            newWo.setConcessionItems(wo.getConcessionItems());
            newWo.setConcessionReason(wo.getConcessionReason());
            newWo.setConcessionPlannedTime(wo.getConcessionPlannedTime());
            newWo.setConcessionTime(wo.getConcessionTime());
            newWo.setConcessionUser(wo.getConcessionUser());
            newWo.setConcessionResolved(0);
        }
        newWo.setOwnerUser(loginUser != null ? loginUser.getUsername() : wo.getOwnerUser());
        newWo.setOwnerUserName(loginUser != null ? loginUser.getRealName() : wo.getOwnerUserName());
        newWo.setCurrentOwnerUser(financeOwner);
        newWo.setCreateBy(loginUser != null ? loginUser.getUsername() : null);
        newWo.setCreateByName(loginUser != null ? loginUser.getRealName() : null);
        newWo.setCreateTime(now);
        newWo.setUpdateTime(now);
        workOrderManager.save(newWo);

        copyOpeningBalanceAttachments(wo.getId(), newWo.getId());

        WorkOrderStep newStep = new WorkOrderStep();
        newStep.setTenantId(wo.getTenantId());
        newStep.setWorkOrderId(newWo.getId());
        newStep.setStepCode(WorkOrderStepCode.FINANCE_RECEIVE.name());
        newStep.setStepName(WorkOrderStepCode.FINANCE_RECEIVE.getLabel());
        newStep.setSort(1);
        newStep.setIsCurrent(1);
        newStep.setStatus(WorkOrderStepStatus.PENDING.name());
        newStep.setOwnerUser(financeOwner);
        newStep.setStartTime(now);
        newStep.setCreateTime(now);
        newStep.setUpdateTime(now);
        stepManager.save(newStep);

        writeLog(newWo.getId(), null, WorkOrderAction.CREATE,
                null, WorkOrderStatus.PENDING.name(),
                null, financeOwner,
                "由 " + wo.getTaskNo() + " 自动转交，请财务接手处理",
                loginUser, wo.getTenantId());
        notificationService.notifyAssign(newWo, financeOwner);
        log.info("[代账转交] 老任务 {} 已 {}，新建财务跟进任务 {} → {}",
                wo.getTaskNo(), wo.getStatus(), newWo.getTaskNo(), financeOwner);
    }

    /**
     * 二次提交路径：复用已存在的 FINANCE_FOLLOW_UP 工单（WAITING_FIX）。
     *  - 同步老工单最新快照字段（B 类）：title / desc / metaJson / verifiedSections / openingData / accountSet*
     *  - 同步让步状态（C 类）：按老工单 concession_complete 决定复制还是清零
     *  - 重新复制 OPENING_BALANCE 附件（D 类）：删除已有，从老工单 copy 当前所有
     *  - 新增一个 FINANCE_RECEIVE 步骤行（sort=max+1, PENDING），旧 REJECTED 行保留作为历史
     *  - 工单状态 WAITING_FIX → PENDING，currentOwnerUser → 上次会计
     *  - 推送给会计 + 写 RESUBMIT 日志
     */
    private void reactivateFollowUp(WorkOrder wo, WorkOrder followUp, String financeOwner,
                                     LoginUser loginUser, Date now) {
        // 1) 同步 B 类快照字段
        followUp.setTitle(StringUtils.defaultString(wo.getTitle(), "代理记账") + " - 财务跟进");
        followUp.setDescription(wo.getDescription());
        followUp.setMetaJson(wo.getMetaJson());
        followUp.setVerifiedSections(wo.getVerifiedSections());
        followUp.setOpeningDataType(wo.getOpeningDataType());
        followUp.setOpeningDataRemark(wo.getOpeningDataRemark());
        followUp.setAccountSetEnableDate(wo.getAccountSetEnableDate());
        followUp.setAccountSetExpiryDate(wo.getAccountSetExpiryDate());

        // 2) 同步 C 类让步：按老工单当前的 concession_complete 决定复制还是清零
        if (wo.getConcessionComplete() != null && wo.getConcessionComplete() == 1) {
            followUp.setConcessionComplete(1);
            followUp.setConcessionItems(wo.getConcessionItems());
            followUp.setConcessionReason(wo.getConcessionReason());
            followUp.setConcessionPlannedTime(wo.getConcessionPlannedTime());
            followUp.setConcessionTime(wo.getConcessionTime());
            followUp.setConcessionUser(wo.getConcessionUser());
            followUp.setConcessionResolved(0);
        } else {
            followUp.setConcessionComplete(0);
            followUp.setConcessionItems(null);
            followUp.setConcessionReason(null);
            followUp.setConcessionPlannedTime(null);
            followUp.setConcessionTime(null);
            followUp.setConcessionUser(null);
            followUp.setConcessionResolved(0);
        }

        // 3) 状态切换 + currentOwner 沿用上次会计
        String prevStatus = followUp.getStatus();
        followUp.setStatus(WorkOrderStatus.PENDING.name());
        followUp.setCurrentOwnerUser(financeOwner);
        followUp.setUpdateTime(now);
        if (loginUser != null) {
            followUp.setUpdateBy(loginUser.getUsername());
            followUp.setUpdateByName(loginUser.getRealName());
        }
        workOrderManager.updateById(followUp);

        // 4) 同步 D 类附件：删除已有 OPENING_BALANCE，从老工单重新复制
        attachmentManager.lambdaUpdate()
                .eq(WorkOrderAttachment::getWorkOrderId, followUp.getId())
                .eq(WorkOrderAttachment::getFileType, "OPENING_BALANCE")
                .remove();
        copyOpeningBalanceAttachments(wo.getId(), followUp.getId());

        // 5) 新增一行 FINANCE_RECEIVE 步骤（sort = 现有最大 + 1，isCurrent=1, PENDING）
        WorkOrderStep maxStep = stepManager.lambdaQuery()
                .eq(WorkOrderStep::getWorkOrderId, followUp.getId())
                .orderByDesc(WorkOrderStep::getSort)
                .last("LIMIT 1")
                .one();
        Integer maxSort = (maxStep == null || maxStep.getSort() == null) ? 0 : maxStep.getSort();
        WorkOrderStep newReceive = new WorkOrderStep();
        newReceive.setTenantId(followUp.getTenantId());
        newReceive.setWorkOrderId(followUp.getId());
        newReceive.setStepCode(WorkOrderStepCode.FINANCE_RECEIVE.name());
        newReceive.setStepName(WorkOrderStepCode.FINANCE_RECEIVE.getLabel());
        newReceive.setSort(maxSort + 1);
        newReceive.setIsCurrent(1);
        newReceive.setStatus(WorkOrderStepStatus.PENDING.name());
        newReceive.setOwnerUser(financeOwner);
        newReceive.setStartTime(now);
        newReceive.setCreateTime(now);
        newReceive.setUpdateTime(now);
        stepManager.save(newReceive);

        // 6) 写 RESUBMIT 日志 + 推送给会计
        writeLog(followUp.getId(), newReceive.getId(), WorkOrderAction.RESUBMIT,
                prevStatus, WorkOrderStatus.PENDING.name(),
                wo.getCurrentOwnerUser(), financeOwner,
                "管家已修正，请重新审核（来自源工单 " + wo.getTaskNo() + "）",
                loginUser, followUp.getTenantId());
        notificationService.notifyAssign(followUp, financeOwner);
        log.info("[财务跟进复用] oldWo={} 二次提交，followUp={} {} → PENDING，会计={}",
                wo.getTaskNo(), followUp.getTaskNo(), prevStatus, financeOwner);
    }

    /** 复制源工单上所有 OPENING_BALANCE 附件元数据到目标工单（指向同一物理文件）。 */
    private void copyOpeningBalanceAttachments(Long fromWorkOrderId, Long toWorkOrderId) {
        List<WorkOrderAttachment> openingFiles = attachmentManager.lambdaQuery()
                .eq(WorkOrderAttachment::getWorkOrderId, fromWorkOrderId)
                .eq(WorkOrderAttachment::getFileType, "OPENING_BALANCE")
                .list();
        for (WorkOrderAttachment src : openingFiles) {
            WorkOrderAttachment copy = new WorkOrderAttachment();
            copy.setTenantId(src.getTenantId());
            copy.setWorkOrderId(toWorkOrderId);
            copy.setStepId(null);
            copy.setFileType(src.getFileType());
            copy.setFileName(src.getFileName());
            copy.setFileUrl(src.getFileUrl());
            copy.setFileSize(src.getFileSize());
            copy.setMimeType(src.getMimeType());
            copy.setUploadUser(src.getUploadUser());
            copy.setUploadUserName(src.getUploadUserName());
            copy.setUploadTime(src.getUploadTime());
            attachmentManager.save(copy);
        }
    }

    /**
     * 找指定财务跟进单上"上次会计"——即历史 FINANCE_RECEIVE step 中 sort 最大那行的 ownerUser。
     * 用于二次提交时沿用首次审核的会计。
     */
    private String findLastFinanceReceiverOwner(Long followUpId) {
        WorkOrderStep last = stepManager.lambdaQuery()
                .eq(WorkOrderStep::getWorkOrderId, followUpId)
                .eq(WorkOrderStep::getStepCode, WorkOrderStepCode.FINANCE_RECEIVE.name())
                .orderByDesc(WorkOrderStep::getSort)
                .last("LIMIT 1")
                .one();
        return last == null ? null : last.getOwnerUser();
    }

    /**
     * 退款 / 服务转移工单首节点（REFUND_REVIEW）被驳回时的回滚。
     * <ul>
     *   <li>REFUND：原销售订单 → PAID；物理删除退款订单 + 明细 + refund_order_info</li>
     *   <li>SERVICE_TRANSFER：物理删除该工单下的 service_transfer_item（合同驱动版无 Order/RefundOrderInfo）</li>
     * </ul>
     */
    private void rollbackRefundOnReject(WorkOrder wo) {
        try {
            // SERVICE_TRANSFER 分支：只删 service_transfer_item
            if (WorkOrderTaskType.SERVICE_TRANSFER.name().equals(wo.getTaskType())) {
                com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.qykj.finmaster.order.domain.ServiceTransferItem> stiQuery =
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
                stiQuery.eq("work_order_id", wo.getId());
                int stiDeleted = serviceTransferItemManager.getBaseMapper().delete(stiQuery);
                log.info("[服务转移驳回回滚] workOrderId={}, 物理删除 service_transfer_item {} 行",
                        wo.getId(), stiDeleted);
                return;
            }

            // REFUND 分支：原有逻辑保持不变
            Long refundOrderId = wo.getOrderId();
            if (refundOrderId == null) {
                log.warn("[退款驳回] workOrderId={} 没有 orderId，跳过订单回滚", wo.getId());
                return;
            }
            com.qykj.finmaster.order.domain.Order refundOrder = renewalOrderManager.getById(refundOrderId);
            if (refundOrder == null) {
                log.warn("[退款驳回] 退款订单已不存在 orderId={}", refundOrderId);
                return;
            }
            // 1) 原订单状态：→ 已付款（无条件更新）
            String sourceOrderNo = refundOrder.getSourceOrderNo();
            if (StringUtils.isNotBlank(sourceOrderNo)) {
                renewalOrderManager.lambdaUpdate()
                        .eq(com.qykj.finmaster.order.domain.Order::getOrderNo, sourceOrderNo)
                        .set(com.qykj.finmaster.order.domain.Order::getStatus,
                                com.qykj.finmaster.common.core.domain.constansts.DictConstants.ORDER_STATUS_PAID)
                        .update();
            }
            // 2) 物理删除退款订单明细
            orderDetailManager.lambdaUpdate()
                    .eq(com.qykj.finmaster.order.domain.OrderDetail::getOrderId, refundOrderId)
                    .remove();
            // 3) 物理删除退款订单本体
            renewalOrderManager.removeById(refundOrderId);
            // 4) 物理删除 refund_order_info（@TableLogic 绕过）
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<com.qykj.finmaster.order.domain.RefundOrderInfo> roiQuery =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            roiQuery.eq("order_no", refundOrder.getOrderNo());
            refundOrderInfoManager.getBaseMapper().delete(roiQuery);
            log.info("[退款驳回回滚] workOrderId={}, 原订单 {} → PAID，退款订单 {} 物理删除",
                    wo.getId(), sourceOrderNo, refundOrder.getOrderNo());
        } catch (Exception e) {
            log.error("[退款驳回] 回滚失败 workOrderId={}", wo.getId(), e);
        }
    }

    /* ==================== 挂起 / 恢复 ==================== */

    @Transactional(rollbackFor = Exception.class)
    public void suspendWorkOrder(SuspendWorkOrderReq req, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(req.getWorkOrderId());
        assertNotWaitingFix(wo);
        if (!WorkOrderStatus.RUNNING.name().equals(wo.getStatus())
                && !WorkOrderStatus.WAITING_CHILD.name().equals(wo.getStatus())) {
            throw new BusinessException("只能挂起运行中的任务");
        }
        if (StringUtils.isBlank(req.getSuspendReason())) {
            throw new BusinessException("请选择挂起原因");
        }
        if (req.getPlannedResumeTime() == null) {
            throw new BusinessException("请选择计划处理时间");
        }
        String prev = wo.getStatus();
        wo.setStatus(WorkOrderStatus.SUSPENDED.name());
        wo.setSuspendReason(req.getSuspendReason());
        wo.setSuspendRemark(req.getRemark());
        wo.setPlannedResumeTime(req.getPlannedResumeTime());
        wo.setUpdateTime(new Date());
        workOrderManager.updateById(wo);
        // 日志正文：原因 + 计划时间 + 备注
        String logContent = String.format("挂起原因：%s；计划处理时间：%s%s",
                suspendReasonLabel(req.getSuspendReason()),
                req.getPlannedResumeTime(),
                StringUtils.isNotBlank(req.getRemark()) ? "；备注：" + req.getRemark() : "");
        writeLog(wo.getId(), null, WorkOrderAction.SUSPEND, prev,
                WorkOrderStatus.SUSPENDED.name(), null, null,
                logContent, loginUser, wo.getTenantId());
    }

    private String suspendReasonLabel(String code) {
        if (code == null) return "";
        switch (code) {
            case "CUSTOMER_ABNORMAL": return "客户异常";
            case "NO_TAX":            return "未开税";
            case "NO_CONTACT":        return "联系不上";
            default:                  return code;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resumeWorkOrder(SuspendWorkOrderReq req, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(req.getWorkOrderId());
        assertNotWaitingFix(wo);
        if (!WorkOrderStatus.SUSPENDED.name().equals(wo.getStatus())) {
            throw new BusinessException("仅挂起状态可恢复");
        }
        wo.setStatus(WorkOrderStatus.RUNNING.name());
        // 恢复时清空挂起字段
        wo.setSuspendReason(null);
        wo.setSuspendRemark(null);
        wo.setPlannedResumeTime(null);
        wo.setUpdateTime(new Date());
        workOrderManager.updateById(wo);
        writeLog(wo.getId(), null, WorkOrderAction.RESUME,
                WorkOrderStatus.SUSPENDED.name(), WorkOrderStatus.RUNNING.name(),
                null, null, req.getRemark(), loginUser, wo.getTenantId());
    }

    /* ==================== 取消（仅 PENDING + 发起人，级联子任务） ==================== */

    @Transactional(rollbackFor = Exception.class)
    public void cancelWorkOrder(CancelWorkOrderReq req, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(req.getWorkOrderId());
        assertNotWaitingFix(wo);  // WAITING_FIX 期间不允许主动取消财务跟进单（必须先取消源工单触发级联）
        if (!WorkOrderStatus.PENDING.name().equals(wo.getStatus())) {
            throw new BusinessException("仅 PENDING 状态任务可取消");
        }
        if (!Objects.equals(wo.getOwnerUser(), loginUser.getUsername())) {
            throw new BusinessException("只有任务发起人可以取消");
        }
        // 老工单（BOOKKEEPING + 无 subType）取消时级联取消其派生的 FINANCE_FOLLOW_UP（即使后者在 WAITING_FIX）
        if (WorkOrderTaskType.BOOKKEEPING.name().equals(wo.getTaskType())
                && StringUtils.isBlank(wo.getSubType())) {
            WorkOrder followUp = findFinanceFollowUp(wo.getId());
            if (followUp != null && !WorkOrderStatus.valueOf(followUp.getStatus()).isTerminal()) {
                cascadeCancel(followUp, "源工单取消级联", loginUser);
            }
        }
        cascadeCancel(wo, req.getReason(), loginUser);
    }

    private void cascadeCancel(WorkOrder wo, String reason, LoginUser loginUser) {
        Date now = new Date();
        String prev = wo.getStatus();
        wo.setStatus(WorkOrderStatus.CANCELED.name());
        wo.setCancelTime(now);
        wo.setCancelReason(reason);
        wo.setUpdateTime(now);
        workOrderManager.updateById(wo);
        writeLog(wo.getId(), null, WorkOrderAction.CANCEL,
                prev, WorkOrderStatus.CANCELED.name(),
                null, null, "取消：" + StringUtils.defaultString(reason),
                loginUser, wo.getTenantId());

        // 级联取消未结束的子任务
        List<WorkOrder> children = listChildren(wo.getId());
        for (WorkOrder c : children) {
            if (!WorkOrderStatus.valueOf(c.getStatus()).isTerminal()) {
                cascadeCancel(c, "父任务取消级联", loginUser);
            }
        }
    }

    /**
     * 续费任务列表：按 companyServiceId 查所有续费任务（含 CANCELED）。
     * 用于公司续费列表 "查看续费任务列表" 按钮，支持一个服务多次续费追溯。
     */
    public List<WorkOrder> listRenewalByCompanyService(Long companyServiceId) {
        if (companyServiceId == null) return java.util.Collections.emptyList();
        return workOrderManager.lambdaQuery()
                .eq(WorkOrder::getTaskType, WorkOrderTaskType.RENEWAL.name())
                .eq(WorkOrder::getSourceType, WorkOrderSourceType.COMPANY_SERVICE.name())
                .eq(WorkOrder::getSourceId, companyServiceId)
                .orderByDesc(WorkOrder::getId)
                .list();
    }

    /* ==================== 数据收集（银行流水上传）批量下发 + 公司列表 ==================== */

    @org.springframework.beans.factory.annotation.Autowired
    private com.qykj.finmaster.customer.company.service.BankStatementService bankStatementService;

    /**
     * 数据收集：按管家批量下发任务。
     * 每个管家一条工单，工单不直接保存负责的公司列表（运行时按 owner_user 查 company_service.butler_owner_user 解析）。
     * metaJson: {"period":"YYYY-MM-01","kind":"BANK_STATEMENT"}
     */
    @Transactional(rollbackFor = Exception.class)
    public List<WorkOrder> batchDispatchDataCollection(
            com.qykj.finmaster.workorder.domain.req.DataCollectionDispatchReq req,
            LoginUser loginUser, Long tenantId) {
        if (req.getAssignees() == null || req.getAssignees().isEmpty()) {
            throw new BusinessException("请至少选择一名管家");
        }
        String period = StringUtils.isNotBlank(req.getPeriod()) ? req.getPeriod() : defaultPeriod();
        com.alibaba.fastjson.JSONObject meta = new com.alibaba.fastjson.JSONObject();
        meta.put("period", period);
        meta.put("kind", "BANK_STATEMENT");
        String metaJson = meta.toJSONString();

        List<WorkOrder> created = new ArrayList<>();
        for (String assignee : req.getAssignees()) {
            if (StringUtils.isBlank(assignee)) continue;
            CreateWorkOrderReq cr = new CreateWorkOrderReq();
            cr.setTaskType(WorkOrderTaskType.DATA_COLLECTION.name());
            cr.setSourceType(WorkOrderSourceType.MANUAL.name());
            cr.setTitle("数据收集 - 银行流水(" + period + ") - " + assignee);
            cr.setRemark(req.getRemark());
            cr.setAssignedOwnerUser(assignee);
            cr.setMetaJson(metaJson);
            if (req.getDueTime() != null) {
                long diff = req.getDueTime().getTime() - System.currentTimeMillis();
                int hours = (int) Math.max(1, diff / 3600_000L);
                cr.setSlaHours(hours);
            }
            WorkOrder wo = createWorkOrder(cr, loginUser, tenantId);
            notificationService.notifyAssign(wo, assignee);
            created.add(wo);
        }
        return created;
    }

    /**
     * 数据收集：列出指定工单负责管家的所有公司及其本期银行流水状态。
     * 数据源：company_service（butler_owner_user = wo.current_owner_user，即被指派的管家）
     *        + company_bank_statement_upload（period 来自 metaJson）。
     * 不在工单表里物化保存公司列表，每次查询时实时聚合。
     *
     * 注意：wo.owner_user 是任务"发起人"（主管），wo.current_owner_user 才是被指派的"管家"，
     * 这里要查管家名下的公司，必须用 current_owner_user。
     */
    public java.util.Map<String, Object> listDataCollectionCompanies(Long workOrderId) {
        WorkOrder wo = workOrderManager.getById(workOrderId);
        if (wo == null) throw new BusinessException("任务不存在");
        if (!WorkOrderTaskType.DATA_COLLECTION.name().equals(wo.getTaskType())) {
            throw new BusinessException("当前任务不是数据收集任务");
        }
        String period = defaultPeriod();
        if (StringUtils.isNotBlank(wo.getMetaJson())) {
            try {
                com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSON.parseObject(wo.getMetaJson());
                if (obj != null && StringUtils.isNotBlank(obj.getString("period"))) {
                    period = obj.getString("period");
                }
            } catch (Exception ignore) { /* fallback to defaultPeriod */ }
        }
        // 被指派的管家（current_owner_user），不是任务发起人（owner_user）
        String butler = StringUtils.isNotBlank(wo.getCurrentOwnerUser())
                ? wo.getCurrentOwnerUser()
                : wo.getOwnerUser();

        // 取该管家负责的所有公司（company_service 视角，记账户 + 非作废）
        List<com.qykj.finmaster.customer.company.domain.CompanyService> services =
                companyServiceManager.lambdaQuery()
                        .eq(com.qykj.finmaster.customer.company.domain.CompanyService::getButlerOwnerUser, butler)
                        .list();
        java.util.Set<Long> companyIds = services.stream()
                .map(com.qykj.finmaster.customer.company.domain.CompanyService::getCompanyId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
        if (!companyIds.isEmpty()) {
            java.util.List<com.qykj.finmaster.customer.company.domain.CustomerCompany> companies =
                    customerCompanyManager.lambdaQuery()
                            .in(com.qykj.finmaster.customer.company.domain.CustomerCompany::getId, companyIds)
                            // 只收集"记账户"公司（is_bookkeeping=1），口径对齐公司维护页"记账户"统计（CompanyServiceServiceImpl.getBankStatementStat）
                            .eq(com.qykj.finmaster.customer.company.domain.CustomerCompany::getIsBookkeeping,
                                    com.qykj.finmaster.common.core.domain.constansts.GlobalConstant.Number.ONE_INT)
                            // 排除合作取消 / 注销公司，放行 NULL（口径对齐 CompanyServiceMapper.xml:201）
                            .and(w -> w.isNull(com.qykj.finmaster.customer.company.domain.CustomerCompany::getCompanyStatus)
                                    .or().notIn(com.qykj.finmaster.customer.company.domain.CustomerCompany::getCompanyStatus,
                                            com.qykj.finmaster.common.core.domain.constansts.DictConstants.COMPANY_STATUS_CANCELLED,
                                            com.qykj.finmaster.common.core.domain.constansts.DictConstants.COMPANY_STATUS_DEREGISTERED))
                            .list();
            java.util.Map<Long, com.qykj.finmaster.customer.company.domain.CompanyBankStatementUpload> stateMap =
                    bankStatementService.getStateMap(companyIds, period);
            for (var cc : companies) {
                java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
                r.put("companyId", cc.getId());
                r.put("companyName", cc.getCompanyName());
                r.put("taxNumber", cc.getTaxNumber());
                var st = stateMap.get(cc.getId());
                int uploadCount = st != null && st.getUploadCount() != null ? st.getUploadCount() : 0;
                int noStatement = st != null && st.getNoStatement() != null ? st.getNoStatement() : 0;
                r.put("uploadCount", uploadCount);
                r.put("noStatement", noStatement);
                // status: DONE / NO_STATEMENT / PENDING
                String status = noStatement == 1 ? "NO_STATEMENT"
                        : (uploadCount > 0 ? "DONE" : "PENDING");
                r.put("status", status);
                rows.add(r);
            }
            // 排序：未完成在前
            rows.sort((a, b) -> {
                int rankA = "PENDING".equals(a.get("status")) ? 0 : 1;
                int rankB = "PENDING".equals(b.get("status")) ? 0 : 1;
                if (rankA != rankB) return rankA - rankB;
                return String.valueOf(a.get("companyName")).compareTo(String.valueOf(b.get("companyName")));
            });
        }

        long doneCount = rows.stream().filter(r -> !"PENDING".equals(r.get("status"))).count();
        java.util.Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("period", period);
        resp.put("butler", butler);
        resp.put("total", rows.size());
        resp.put("doneCount", doneCount);
        resp.put("companies", rows);
        return resp;
    }

    /** 默认账期：15 号前为上月 1 号，否则当月 1 号。与 BankStatementService 保持一致。 */
    private String defaultPeriod() {
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate p = today.getDayOfMonth() < 15
                ? today.minusMonths(1).withDayOfMonth(1)
                : today.withDayOfMonth(1);
        return p.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /* ==================== 批量下发续费工单（公司续费列表多选后调用） ==================== */

    @Transactional(rollbackFor = Exception.class)
    public List<WorkOrder> batchDispatchRenewal(
            com.qykj.finmaster.workorder.domain.req.BatchDispatchRenewalReq req,
            LoginUser loginUser, Long tenantId) {
        if (req.getTargets() == null || req.getTargets().isEmpty()) {
            throw new BusinessException("请至少选择一家公司");
        }
        if (StringUtils.isBlank(req.getAssignedOwnerUser())) {
            throw new BusinessException("请选择处理人");
        }
        List<WorkOrder> created = new ArrayList<>();
        for (var t : req.getTargets()) {
            if (t.getCompanyId() == null) continue;
            // 兜底反查 customerId（公司表里有）和最近一份非作废合同 contractId
            Long customerId = t.getCustomerId();
            Long contractId = t.getContractId();
            String companyName = t.getCompanyName();
            CustomerCompany cc = customerCompanyManager.getById(t.getCompanyId());
            if (cc != null) {
                if (customerId == null) customerId = cc.getCustomerId();
                if (StringUtils.isBlank(companyName)) companyName = cc.getCompanyName();
            }
            if (contractId == null) {
                Contract latest = contractManager.lambdaQuery()
                        .eq(Contract::getCompanyId, t.getCompanyId())
                        .ne(Contract::getStatus, com.qykj.finmaster.common.core.domain.constansts.DictConstants.CONTRACT_STATUS_DISUSED)
                        .eq(Contract::getTenantId, tenantId)
                        .orderByDesc(Contract::getCreateTime)
                        .last("LIMIT 1")
                        .one();
                if (latest != null) contractId = latest.getId();
            }
            CreateWorkOrderReq cr = new CreateWorkOrderReq();
            cr.setTaskType(WorkOrderTaskType.RENEWAL.name());
            // 续费任务优先按 companyServiceId 关联（支持按服务追溯多次续费）；缺失时退化为合同/手工
            if (t.getCompanyServiceId() != null) {
                cr.setSourceType(WorkOrderSourceType.COMPANY_SERVICE.name());
                cr.setSourceId(t.getCompanyServiceId());
            } else {
                cr.setSourceType(contractId != null
                        ? WorkOrderSourceType.CONTRACT.name()
                        : WorkOrderSourceType.MANUAL.name());
            }
            cr.setContractId(contractId);
            cr.setCompanyId(t.getCompanyId());
            cr.setCustomerId(customerId);
            cr.setTitle("续费 - " + StringUtils.defaultString(companyName, "公司 #" + t.getCompanyId()));
            cr.setRemark(req.getRemark());
            cr.setAssignedOwnerUser(req.getAssignedOwnerUser());
            if (req.getDueTime() != null) {
                long diff = req.getDueTime().getTime() - System.currentTimeMillis();
                int hours = (int) Math.max(1, diff / 3600_000L);
                cr.setSlaHours(hours);
            }
            WorkOrder wo = createWorkOrder(cr, loginUser, tenantId);
            notificationService.notifyAssign(wo, req.getAssignedOwnerUser());
            created.add(wo);
        }
        return created;
    }

    /* ==================== 续费任务完成：自动建续费订单 + 续费合同 ==================== */

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.qykj.finmaster.order.manager.impl.OrderManagerImpl renewalOrderManager;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.qykj.finmaster.order.manager.impl.OrderDetailManagerImpl renewalOrderDetailManager;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.qykj.finmaster.contract.manager.ContractDetailManager renewalContractDetailManager;

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder completeRenewal(com.qykj.finmaster.workorder.domain.req.CompleteRenewalReq req,
                                     LoginUser loginUser, Long tenantId) {
        WorkOrder wo = getAndCheck(req.getWorkOrderId());
        if (!WorkOrderTaskType.RENEWAL.name().equals(wo.getTaskType())) {
            throw new BusinessException("仅续费任务可走此完成流程");
        }
        if (WorkOrderStatus.DONE.name().equals(wo.getStatus())
                || WorkOrderStatus.CANCELED.name().equals(wo.getStatus())) {
            throw new BusinessException("任务已结束");
        }
        WorkOrderStep current = getCurrentStep(wo.getId());
        if (current == null) throw new BusinessException("找不到当前节点");
        if (!Objects.equals(current.getOwnerUser(), loginUser.getUsername())) {
            throw new BusinessException("只有节点持有人可以完成");
        }
        if (req.getContractPrice() == null || req.getContractPrice().signum() <= 0) {
            throw new BusinessException("续费金额必须大于 0");
        }
        if (StringUtils.isBlank(req.getPaymentType())) {
            throw new BusinessException("支付方式不能为空");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new BusinessException("产品明细不能为空，请确认服务信息已配置产品");
        }

        // 解析业务 ID（前端没传时取原合同的 businessId）
        Long businessId = req.getBusinessId();
        Contract originContract = wo.getContractId() != null ? contractManager.getById(wo.getContractId()) : null;
        if (businessId == null && originContract != null) businessId = originContract.getBusinessId();
        if (businessId == null) {
            throw new BusinessException("无法确定业务类型，请在请求中传入 businessId 或确保原合同有效");
        }

        Date now = new Date();

        // ============== 1) 创建续费订单 ==============
        com.qykj.finmaster.order.domain.Order order = new com.qykj.finmaster.order.domain.Order();
        order.setTenantId(tenantId);
        order.setOrderNo(generateRenewalOrderNo());
        order.setOrderType(com.qykj.finmaster.common.core.domain.constansts.DictConstants.ORDER_TYPE_NORMAL);
        // 订单状态由前端选择（默认 PAID）
        order.setStatus(StringUtils.isNotBlank(req.getOrderStatus())
                ? req.getOrderStatus()
                : com.qykj.finmaster.common.core.domain.constansts.DictConstants.ORDER_STATUS_PAID);
        order.setCustomerId(wo.getCustomerId());
        // orders.customer_name 字段 NOT NULL，没传时反查客户名
        if (wo.getCustomerId() != null) {
            CustomerInfo ci = customerInfoManager.getById(wo.getCustomerId());
            if (ci != null) order.setCustomerName(ci.getCustomerName());
        }
        if (StringUtils.isBlank(order.getCustomerName())) {
            order.setCustomerName("续费客户 #" + wo.getCustomerId());
        }
        order.setCompanyId(wo.getCompanyId());
        // 续费金额：订单的 totalAmount / finalAmount / paidAmount 与合同金额完全一致
        order.setTotalAmount(req.getContractPrice());
        order.setFinalAmount(req.getContractPrice());
        order.setPaidAmount(req.getContractPrice());
        // orders 表中下面这些字段都是 NOT NULL 无默认值，统一兜底为 0
        order.setTaxAmount(java.math.BigDecimal.ZERO);
        order.setAfterTaxAmount(req.getContractPrice());
        order.setDiscount(0);
        order.setPaymentType(req.getPaymentType());
        // Order.payTime 是 LocalDateTime，Req 传 LocalDate；按当天 00:00:00 转换
        if (req.getPayTime() != null) {
            order.setPayTime(req.getPayTime().atStartOfDay());
        }
        order.setRemark("续费任务自动生成（来源工单 " + wo.getTaskNo() + "）");
        // 创建人 / 负责人：续费完成时由当前操作用户（通常是管家）承担
        order.setCreateBy(loginUser.getUsername());
        order.setCreateByName(loginUser.getRealName());
        order.setUpdateBy(loginUser.getUsername());
        order.setUpdateByName(loginUser.getRealName());
        order.setCreateTime(now);
        order.setUpdateTime(now);
        renewalOrderManager.save(order);

        // 订单明细：根据 items 落库；空时退化为单条总额明细
        List<com.qykj.finmaster.order.domain.OrderDetail> orderDetailList = new ArrayList<>();
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            for (var it : req.getItems()) {
                com.qykj.finmaster.order.domain.OrderDetail od = new com.qykj.finmaster.order.domain.OrderDetail();
                od.setTenantId(tenantId);
                od.setOrderId(order.getId());
                od.setCompanyId(it.getCompanyId() != null ? it.getCompanyId() : wo.getCompanyId());
                od.setBusinessId(it.getBusinessId() != null ? it.getBusinessId() : businessId);
                if (it.getProductId() == null) {
                    throw new BusinessException("产品ID缺失，请先在服务中关联产品再续费");
                }
                od.setProductId(it.getProductId());
                od.setProductName(it.getProductName());
                od.setQuantity(it.getQuantity() != null ? it.getQuantity() : 1);
                java.math.BigDecimal itemAmount = it.getAmount() != null ? it.getAmount() : req.getContractPrice();
                od.setAmount(itemAmount);
                od.setUnitPrice(it.getUnitPrice() != null ? it.getUnitPrice() : itemAmount);
                od.setDiscount(0);
                od.setTaxRate(java.math.BigDecimal.ZERO);
                od.setOrderDetailType(com.qykj.finmaster.common.core.domain.constansts.DictConstants.ORDER_DETAIL_TYPE_RENEW);
                od.setCreateBy(loginUser.getUsername());
                od.setCreateByName(loginUser.getRealName());
                od.setCreateTime(now);
                renewalOrderDetailManager.save(od);
                orderDetailList.add(od);
            }
        } else {
            com.qykj.finmaster.order.domain.OrderDetail od = new com.qykj.finmaster.order.domain.OrderDetail();
            od.setTenantId(tenantId);
            od.setOrderId(order.getId());
            od.setCompanyId(wo.getCompanyId());
            od.setBusinessId(businessId);
            od.setQuantity(1);
            od.setAmount(req.getContractPrice());
            od.setUnitPrice(req.getContractPrice());
            od.setDiscount(0);
            od.setTaxRate(java.math.BigDecimal.ZERO);
            od.setOrderDetailType(com.qykj.finmaster.common.core.domain.constansts.DictConstants.ORDER_DETAIL_TYPE_RENEW);
            od.setCreateBy(loginUser.getUsername());
            od.setCreateByName(loginUser.getRealName());
            od.setCreateTime(now);
            renewalOrderDetailManager.save(od);
            orderDetailList.add(od);
        }

        // ============== 2) 创建续费合同 ==============
        Contract contract = new Contract();
        contract.setTenantId(tenantId);
        contract.setContractNo(generateRenewalContractNo());
        contract.setCompanyId(wo.getCompanyId());
        // Contract 没有 customerId 字段（业务方按 companyId 反查所属客户），略过
        contract.setBusinessId(businessId);
        contract.setContractType(com.qykj.finmaster.common.core.domain.constansts.DictConstants.CONTRACT_TYPE_RENEW);
        // Contract.contractStartTime / endTime 是 java.util.Date，Req 传的是 LocalDateTime，做转换
        if (req.getContractStartTime() != null) {
            contract.setContractStartTime(java.util.Date.from(
                    req.getContractStartTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        if (req.getContractEndTime() != null) {
            contract.setContractEndTime(java.util.Date.from(
                    req.getContractEndTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        }
        contract.setContractPrice(req.getContractPrice());
        contract.setStatus(com.qykj.finmaster.common.core.domain.constansts.DictConstants.CONTRACT_STATUS_COMPLETED);
        // 标记此合同已关联订单，前端合同列表 haveOrder=1 即可显示"已关联"标签
        contract.setHaveOrder(1);
        contract.setRemark("续费任务自动生成（工单 " + wo.getTaskNo() + "，订单 " + order.getOrderNo() + "）");
        // 创建人 / 负责人：续费完成时由当前操作用户（通常是管家）承担
        contract.setCreateBy(loginUser.getUsername());
        contract.setCreateByName(loginUser.getRealName());
        contract.setUpdateBy(loginUser.getUsername());
        contract.setUpdateByName(loginUser.getRealName());
        contract.setCreateTime(now);
        contract.setUpdateTime(now);
        contractManager.save(contract);

        // 合同明细：把刚建的订单明细挂到合同上
        for (var od : orderDetailList) {
            com.qykj.finmaster.contract.domain.ContractDetail cd = new com.qykj.finmaster.contract.domain.ContractDetail();
            cd.setTenantId(tenantId);
            cd.setContractId(contract.getId());
            cd.setOrderNo(order.getOrderNo());
            cd.setOrderDetailId(od.getId());
            cd.setProductId(od.getProductId());
            cd.setCreateBy(loginUser.getUsername());
            cd.setCreateByName(loginUser.getRealName());
            cd.setCreateTime(now);
            renewalContractDetailManager.save(cd);
        }

        // ============== 2.5) 回写订单 ID 到工单 + 持久化账套截止日期 ==============
        // 续费产生的 order 必须挂回工单，否则工作台续费金额聚合（按 work_order.order_id JOIN orders）查不到
        wo.setOrderId(order.getId());
        if (req.getAccountSetExpiryDate() != null) {
            wo.setAccountSetExpiryDate(req.getAccountSetExpiryDate());
        }
        workOrderManager.updateById(wo);

        // ============== 2.6) 同步续费副作用 ==============
        // 与 ContractServiceImpl.updateContractStatus 中"续费合同变为完成"分支保持同样的副作用：
        //   1) INSERT company_service_renew（续费历史，由 CS 工作台 / 续费列表 / 报表读取）
        //   2) UPDATE company_service.actual_end_date（服务实际到期时间，影响"剩余服务时长"、到期提醒等）
        //   3) 清空 company_service.pending_renewal_*（关闭主管派给管家的"待办续费任务"）
        // 因为本路径直接 contractManager.save(contract) 把合同 status 写成 COMPLETED，绕过了 updateContractStatus，
        // 所以那段副作用必须在这里手动重放一次，否则续费完成后续费历史 / 实际到期日 / 待办列表全部不更新。
        applyRenewalSideEffectsFromWorkOrder(wo, contract, businessId, req.getContractPrice(), loginUser);

        // ============== 3) 关闭工单 ==============
        ExecuteStepReq exec = new ExecuteStepReq();
        exec.setWorkOrderId(wo.getId());
        exec.setRemark("续费完成 · 已自动生成订单 " + order.getOrderNo() + " 与合同 " + contract.getContractNo());
        executeCurrentStep(exec, loginUser);

        log.info("[续费完成] workOrderId={}, orderNo={}, contractNo={}",
                wo.getId(), order.getOrderNo(), contract.getContractNo());
        return wo;
    }

    private String generateRenewalOrderNo() {
        return "REN-" + new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
    private String generateRenewalContractNo() {
        return "CT-REN-" + new java.text.SimpleDateFormat("yyyyMMdd").format(new Date())
                + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    /**
     * 续费工单完成后：写续费历史 + 推服务实际到期日 + 清空"待办续费任务"。
     * 与 {@link com.qykj.finmaster.contract.service.ContractServiceImpl#updateContractStatus} 中
     * "续费合同变为完成"分支同口径；因 completeRenewal 直接 save 合同为 COMPLETED 而绕过了 updateContractStatus，
     * 副作用必须在这里手动重放。
     *
     * @param wo            续费工单
     * @param contract      已建好的续费合同（用于日志/备注里回显合同号）
     * @param businessId    业务ID（用于反查 service_id）
     * @param contractPrice 续费金额，写入 company_service_renew.renew_amount
     * @param loginUser     当前操作人，作为续费记录的创建人
     */
    private void applyRenewalSideEffectsFromWorkOrder(WorkOrder wo,
                                                      Contract contract,
                                                      Long businessId,
                                                      java.math.BigDecimal contractPrice,
                                                      LoginUser loginUser) {
        if (wo == null || wo.getCompanyId() == null || businessId == null) {
            log.warn("[续费副作用] 缺少 companyId / businessId，跳过 workOrderId={}", wo == null ? null : wo.getId());
            return;
        }
        // 1) businessId → service_id 集合
        List<Long> serviceIds = serviceBusinessManager.lambdaQuery()
                .eq(ServiceBusiness::getBusinessId, businessId)
                .list().stream()
                .map(ServiceBusiness::getServiceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (serviceIds.isEmpty()) {
            // 业务没绑服务，无法落 company_service_renew；与合同模块同口径，明确报错让外层事务回滚
            throw new BusinessException("当前业务未配置对应服务，无法生成续费记录");
        }
        // 2) 按"同税号公司"扩展，找 company_service
        CustomerCompany self = customerCompanyManager.getById(wo.getCompanyId());
        List<Long> companyIdList;
        if (self != null && StringUtils.isNotBlank(self.getTaxNumber())) {
            companyIdList = customerCompanyManager.lambdaQuery()
                    .eq(CustomerCompany::getTaxNumber, self.getTaxNumber())
                    .list().stream()
                    .map(CustomerCompany::getId)
                    .toList();
        } else {
            companyIdList = java.util.Collections.singletonList(wo.getCompanyId());
        }
        List<CompanyService> companyServices = companyServiceManager.lambdaQuery()
                .in(CompanyService::getCompanyId, companyIdList)
                .in(CompanyService::getServiceId, serviceIds)
                .list();
        if (companyServices.isEmpty()) {
            throw new BusinessException("当前公司不存在公司服务，无法生成续费记录");
        }
        CompanyService cs = companyServices.get(0);

        // 3) 日期校验（与 ContractServiceImpl 同口径）
        java.time.LocalDate renewStart = contract.getContractStartTime() != null
                ? contract.getContractStartTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                : null;
        java.time.LocalDate renewEnd = contract.getContractEndTime() != null
                ? contract.getContractEndTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                : null;
        if (renewStart == null || renewEnd == null) {
            throw new BusinessException("续费开始或结束时间为空，无法生成续费记录");
        }
        java.time.LocalDate actualEnd = cs.getActualEndDate();
        if (actualEnd != null && !renewStart.isAfter(actualEnd)) {
            throw new BusinessException("续费开始日期须晚于实际结束日期");
        }
        if (!renewEnd.isAfter(renewStart)) {
            throw new BusinessException("续费结束日期须晚于续费开始日期");
        }

        // 4) INSERT company_service_renew + UPDATE company_service.actual_end_date（addCompanyRenew 内部一并完成）
        AddCompanyRenewReq renewReq = new AddCompanyRenewReq();
        renewReq.setCompanyServiceId(cs.getId());
        renewReq.setRenewEffectiveDate(renewStart);
        renewReq.setRenewEndDate(renewEnd);
        renewReq.setRenewType(0);   // 0-正常续费；这里走的是"自动从续费工单生成"的标准路径
        renewReq.setRenewAmount(contractPrice != null ? contractPrice : java.math.BigDecimal.ZERO);
        renewReq.setRemark("由续费工单 " + wo.getTaskNo()
                + " 自动生成续费记录（合同 " + contract.getContractNo() + "）");
        companyServiceService.addCompanyRenew(renewReq, loginUser);

        // 5) 清空所有匹配 company_service 上的 pending_renewal_*（关闭主管派给管家的"待办续费任务"）
        try {
            companyServiceManager.lambdaUpdate()
                    .set(CompanyService::getPendingRenewalAssignee, null)
                    .set(CompanyService::getPendingRenewalAssigneeName, null)
                    .set(CompanyService::getPendingRenewalAssignedAt, null)
                    .set(CompanyService::getPendingRenewalNote, null)
                    .in(CompanyService::getId,
                            companyServices.stream().map(CompanyService::getId).toList())
                    .update();
        } catch (Exception e) {
            // 清理失败不阻断主流程（续费历史 / actual_end_date 已落库）
            log.warn("[续费副作用] 清理 pending_renewal_* 失败 workOrderId={}: {}", wo.getId(), e.getMessage());
        }

        log.info("[续费副作用] 工单 {} 续费完成 → company_service.id={} actual_end_date 推到 {}",
                wo.getTaskNo(), cs.getId(), renewEnd);
    }

    /* ==================== 受理（PENDING → RUNNING，仅当前持有人） ==================== */

    @Transactional(rollbackFor = Exception.class)
    public WorkOrder acceptWorkOrder(Long workOrderId, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(workOrderId);
        assertNotWaitingFix(wo);
        if (!WorkOrderStatus.PENDING.name().equals(wo.getStatus())) {
            throw new BusinessException("仅 PENDING 状态可受理");
        }
        WorkOrderStep current = getCurrentStep(wo.getId());
        if (current == null) throw new BusinessException("找不到当前节点");
        if (!Objects.equals(current.getOwnerUser(), loginUser.getUsername())) {
            throw new BusinessException("只有当前节点持有人可受理");
        }
        Date now = new Date();
        current.setStatus(WorkOrderStepStatus.RUNNING.name());
        current.setStartTime(now);
        current.setUpdateTime(now);
        stepManager.updateById(current);
        String prevStatus = wo.getStatus();
        wo.setStatus(WorkOrderStatus.RUNNING.name());
        wo.setUpdateTime(now);
        workOrderManager.updateById(wo);
        writeLog(wo.getId(), current.getId(), WorkOrderAction.ACCEPT,
                prevStatus, WorkOrderStatus.RUNNING.name(),
                null, current.getOwnerUser(),
                "已受理，开始处理", loginUser, wo.getTenantId());
        return wo;
    }

    /* ==================== 撤回（仅发起人，仅 PENDING 未受理） ==================== */

    @Transactional(rollbackFor = Exception.class)
    public void recallWorkOrder(Long workOrderId, String reason, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(workOrderId);
        assertNotWaitingFix(wo);
        if (!WorkOrderStatus.PENDING.name().equals(wo.getStatus())) {
            throw new BusinessException("任务已被受理或已结束，无法撤回");
        }
        if (!Objects.equals(wo.getOwnerUser(), loginUser.getUsername())) {
            throw new BusinessException("只有任务发起人可以撤回");
        }
        // 级联取消所有未结束子任务（与 cancel 行为一致）
        cascadeCancel(wo, StringUtils.defaultString(reason, "发起人撤回"), loginUser);
        // 标记最后日志为撤回（cascadeCancel 写的是 CANCEL，这里追加一条 RECALL 便于审计区分）
        writeLog(wo.getId(), null, WorkOrderAction.RECALL,
                WorkOrderStatus.CANCELED.name(), WorkOrderStatus.CANCELED.name(),
                null, null,
                "撤回：" + StringUtils.defaultString(reason),
                loginUser, wo.getTenantId());
    }

    /* ==================== 催办 ==================== */

    public void urgeWorkOrder(UrgeWorkOrderReq req, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(req.getWorkOrderId());
        assertNotWaitingFix(wo);
        writeLog(wo.getId(), null, WorkOrderAction.URGE,
                wo.getStatus(), wo.getStatus(), null, null,
                req.getContent(), loginUser, wo.getTenantId());
        // WebSocket 推送到当前持有人
        notificationService.notifyUrge(wo,
                loginUser != null ? loginUser.getRealName() : "发起人",
                req.getContent());
    }

    /* ==================== 查询辅助 ==================== */

    public WorkOrder getDetail(Long id) {
        WorkOrder wo = workOrderManager.getById(id);
        if (wo == null) return null;
        // 反查合同号填充虚拟字段，便于前端"合同号"链接显示真实编号
        if (wo.getContractId() != null) {
            Contract c = contractManager.getById(wo.getContractId());
            if (c != null) wo.setContractNo(c.getContractNo());
        }
        fillCustomerCompanyName(java.util.Collections.singletonList(wo));
        return wo;
    }

    /** 批量填充 customerName / companyName 虚拟字段 */
    private void fillCustomerCompanyName(List<WorkOrder> list) {
        if (list == null || list.isEmpty()) return;
        java.util.Set<Long> companyIds = new java.util.HashSet<>();
        java.util.Set<Long> customerIds = new java.util.HashSet<>();
        for (WorkOrder w : list) {
            if (w.getCompanyId() != null) companyIds.add(w.getCompanyId());
            if (w.getCustomerId() != null) customerIds.add(w.getCustomerId());
        }
        java.util.Map<Long, String> companyMap = companyIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : customerCompanyManager.listByIds(companyIds).stream()
                    .collect(java.util.stream.Collectors.toMap(
                        CustomerCompany::getId, CustomerCompany::getCompanyName, (a, b) -> a));
        java.util.Map<Long, String> customerMap = customerIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : customerInfoManager.listByIds(customerIds).stream()
                    .collect(java.util.stream.Collectors.toMap(
                        CustomerInfo::getId, CustomerInfo::getCustomerName, (a, b) -> a));
        for (WorkOrder w : list) {
            if (w.getCompanyId() != null) w.setCompanyName(companyMap.get(w.getCompanyId()));
            if (w.getCustomerId() != null) w.setCustomerName(customerMap.get(w.getCustomerId()));
        }
    }

    public List<WorkOrderStep> listSteps(Long workOrderId) {
        return stepManager.lambdaQuery()
                .eq(WorkOrderStep::getWorkOrderId, workOrderId)
                .orderByAsc(WorkOrderStep::getSort)
                .list();
    }

    public List<WorkOrderLog> listLogs(Long workOrderId) {
        return logManager.lambdaQuery()
                .eq(WorkOrderLog::getWorkOrderId, workOrderId)
                .orderByAsc(WorkOrderLog::getCreateTime)
                .list();
    }

    public List<WorkOrder> listChildren(Long parentId) {
        return workOrderManager.lambdaQuery()
                .eq(WorkOrder::getParentTaskId, parentId)
                .orderByAsc(WorkOrder::getId)
                .list();
    }

    /** 代账派工弹窗预填 */
    public com.qykj.finmaster.workorder.domain.resp.BookkeepingDispatchPreloadResp preloadBookkeepingDispatch(Long companyId) {
        return bookkeepingDispatchHelper.preload(companyId);
    }

    /** 代账任务详情页"按需保存"：未传的块不动 */
    @Transactional(rollbackFor = Exception.class)
    public void saveBookkeepingDispatch(Long companyId,
                                        com.qykj.finmaster.workorder.domain.req.BookkeepingDispatchPayload payload,
                                        LoginUser loginUser, Long tenantId) {
        bookkeepingDispatchHelper.savePartial(companyId, payload, loginUser, tenantId);
    }

    /** 保存期初数据类型 + 说明（必填校验在前端做） */
    @Transactional(rollbackFor = Exception.class)
    public void saveOpeningData(Long workOrderId, Integer type, String remark, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(workOrderId);
        assertNotWaitingFix(wo);
        wo.setOpeningDataType(type);
        wo.setOpeningDataRemark(remark);
        wo.setUpdateTime(new Date());
        if (loginUser != null) {
            wo.setUpdateBy(loginUser.getUsername());
            wo.setUpdateByName(loginUser.getRealName());
        }
        workOrderManager.updateById(wo);
    }

    /** 保存账套启用日期 + 截止日期（代账任务） */
    @Transactional(rollbackFor = Exception.class)
    public void saveAccountSetDates(Long workOrderId, String enableDate, String expiryDate, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(workOrderId);
        assertNotWaitingFix(wo);
        wo.setAccountSetEnableDate(StringUtils.isBlank(enableDate) ? null : java.time.LocalDate.parse(enableDate));
        wo.setAccountSetExpiryDate(StringUtils.isBlank(expiryDate) ? null : java.time.LocalDate.parse(expiryDate));
        wo.setUpdateTime(new Date());
        if (loginUser != null) {
            wo.setUpdateBy(loginUser.getUsername());
            wo.setUpdateByName(loginUser.getRealName());
        }
        workOrderManager.updateById(wo);
    }

    /** 标记任务详情页某区块为"核对无误" */
    @Transactional(rollbackFor = Exception.class)
    public void markSectionVerified(Long workOrderId, String section, Boolean verified, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(workOrderId);
        assertNotWaitingFix(wo);
        com.alibaba.fastjson.JSONObject sections = StringUtils.isBlank(wo.getVerifiedSections())
                ? new com.alibaba.fastjson.JSONObject()
                : com.alibaba.fastjson.JSON.parseObject(wo.getVerifiedSections());
        sections.put(section, Boolean.TRUE.equals(verified));
        wo.setVerifiedSections(sections.toJSONString());
        wo.setUpdateTime(new Date());
        if (loginUser != null) {
            wo.setUpdateBy(loginUser.getUsername());
            wo.setUpdateByName(loginUser.getRealName());
        }
        workOrderManager.updateById(wo);
    }

    /** 客服后期补齐让步项后，标记 concession_resolved=1；
     *  若任务处于 CONCESSION_DONE 状态，自动转 DONE 表示真正完成 */
    @Transactional(rollbackFor = Exception.class)
    public void resolveConcession(Long workOrderId, String remark, LoginUser loginUser) {
        WorkOrder wo = getAndCheck(workOrderId);
        assertNotWaitingFix(wo);
        if (wo.getConcessionComplete() == null || wo.getConcessionComplete() != 1) {
            throw new BusinessException("该任务不是让步完成，无需处理");
        }
        String prevStatus = wo.getStatus();
        wo.setConcessionResolved(1);
        // 若处于让步完成态，转为真完成
        if (WorkOrderStatus.CONCESSION_DONE.name().equals(prevStatus)) {
            wo.setStatus(WorkOrderStatus.DONE.name());
            wo.setCompleteTime(new Date());
        }
        wo.setUpdateTime(new Date());
        workOrderManager.updateById(wo);
        writeLog(workOrderId, null, WorkOrderAction.EXECUTE,
                prevStatus, wo.getStatus(),
                null, null,
                "【让步项已补齐】" + StringUtils.defaultString(remark, ""),
                loginUser, wo.getTenantId());

        // 让步补齐 → 财务系统增量同步：
        //   - 如果账套已存在（会计已完成过 FINANCE_RECEIVE 节点）→ 调 updateAccountSet 把最新数据推过去 + 推员工
        //   - 如果账套不存在（财务还没完成 FINANCE_RECEIVE）→ 跳过，不需要预先调用，
        //     因为会计后续完成 FINANCE_RECEIVE 时会用 CRM 的最新数据走 createAccountSet，
        //     此时已经包含本次补齐的内容，最终结果一致。
        if (wo.getCompanyId() != null && WorkOrderTaskType.BOOKKEEPING.name().equals(wo.getTaskType())) {
            try {
                bookkeepingHelper.syncOnConcessionResolved(wo.getCompanyId(), wo, loginUser);
            } catch (Exception e) {
                log.error("[让步补齐→财务系统] 同步异常 workOrderId={}, companyId={}",
                        wo.getId(), wo.getCompanyId(), e);
            }
        }
    }

    /**
     * 按 taskType + stepCode 统计每个候选用户当前未完成的任务数（用于派工选人时展示工作量）。
     * stepCode 非空 → 统计当前持有人是该用户、且任务卡在指定 step 的工单；
     * stepCode 为空 → 统计该 taskType 下任意 step 该用户当前持有的未完成工单。
     * "未完成"= status IN (DRAFT, PENDING, RUNNING, WAITING_CHILD, SUSPENDED)
     */
    public java.util.Map<String, Long> countPendingByUser(String taskType, String stepCode, Long tenantId) {
        if (StringUtils.isBlank(taskType)) return java.util.Collections.emptyMap();
        java.util.List<String> openStatuses = java.util.Arrays.asList(
                WorkOrderStatus.DRAFT.name(),
                WorkOrderStatus.PENDING.name(),
                WorkOrderStatus.RUNNING.name(),
                WorkOrderStatus.WAITING_CHILD.name(),
                WorkOrderStatus.SUSPENDED.name(),
                // 让步完成：客服仍需补齐让步缺失项，算作"未结束"
                WorkOrderStatus.CONCESSION_DONE.name()
        );
        java.util.List<WorkOrder> list = workOrderManager.lambdaQuery()
                .eq(WorkOrder::getTenantId, tenantId)
                .eq(WorkOrder::getTaskType, taskType)
                .in(WorkOrder::getStatus, openStatuses)
                .isNotNull(WorkOrder::getCurrentOwnerUser)
                .ne(WorkOrder::getCurrentOwnerUser, "")
                .list();
        if (list.isEmpty()) return java.util.Collections.emptyMap();
        // stepCode 非空：进一步过滤当前 step 是该 stepCode（按持有人当前所在 step 判断）
        if (StringUtils.isNotBlank(stepCode)) {
            java.util.List<Long> woIds = list.stream().map(WorkOrder::getId).collect(java.util.stream.Collectors.toList());
            java.util.Set<Long> matched = stepManager.lambdaQuery()
                    .in(WorkOrderStep::getWorkOrderId, woIds)
                    .eq(WorkOrderStep::getStepCode, stepCode)
                    .eq(WorkOrderStep::getIsCurrent, 1)
                    .list().stream().map(WorkOrderStep::getWorkOrderId).collect(java.util.stream.Collectors.toSet());
            list = list.stream().filter(w -> matched.contains(w.getId())).collect(java.util.stream.Collectors.toList());
        }
        return list.stream().collect(java.util.stream.Collectors.groupingBy(
                WorkOrder::getCurrentOwnerUser, java.util.stream.Collectors.counting()));
    }

    /**
     * 按 taskType + stepCode 统计每个候选用户已完成（status=DONE）的任务数（与未完成接口并列，前端展示「未完成/已完成 X/Y」）。
     * 与 {@link #countPendingByUser} 逻辑完全一致，唯一差异：状态过滤改为 status = DONE（只统 DONE，不含 CONCESSION_DONE），
     * 与"未完成含 CONCESSION_DONE"互补、不重不漏。
     */
    public java.util.Map<String, Long> countDoneByUser(String taskType, String stepCode, Long tenantId) {
        if (StringUtils.isBlank(taskType)) return java.util.Collections.emptyMap();
        java.util.List<WorkOrder> list = workOrderManager.lambdaQuery()
                .eq(WorkOrder::getTenantId, tenantId)
                .eq(WorkOrder::getTaskType, taskType)
                .eq(WorkOrder::getStatus, WorkOrderStatus.DONE.name())
                .isNotNull(WorkOrder::getCurrentOwnerUser)
                .ne(WorkOrder::getCurrentOwnerUser, "")
                .list();
        if (list.isEmpty()) return java.util.Collections.emptyMap();
        // stepCode 非空：进一步过滤当前 step 是该 stepCode（按持有人当前所在 step 判断）
        if (StringUtils.isNotBlank(stepCode)) {
            java.util.List<Long> woIds = list.stream().map(WorkOrder::getId).collect(java.util.stream.Collectors.toList());
            java.util.Set<Long> matched = stepManager.lambdaQuery()
                    .in(WorkOrderStep::getWorkOrderId, woIds)
                    .eq(WorkOrderStep::getStepCode, stepCode)
                    .eq(WorkOrderStep::getIsCurrent, 1)
                    .list().stream().map(WorkOrderStep::getWorkOrderId).collect(java.util.stream.Collectors.toSet());
            list = list.stream().filter(w -> matched.contains(w.getId())).collect(java.util.stream.Collectors.toList());
        }
        return list.stream().collect(java.util.stream.Collectors.groupingBy(
                WorkOrder::getCurrentOwnerUser, java.util.stream.Collectors.counting()));
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.qykj.finmaster.system.user.manager.UserManager userManagerForScope;

    /** 列表查询结果 + 总数 */
    @lombok.Data
    public static class QueryListResult {
        private final List<WorkOrder> list;
        private final long total;
    }

    public QueryListResult queryList(QueryWorkOrderReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<WorkOrder> q = buildListQuery(req, tenantId, loginUser, true);
        // 按用户指定排序，默认 create_time DESC；仅允许白名单字段防 SQL 注入
        applyOrderBy(q, req.getOrderByColumn(), req.getIsAsc());

        int pageNum = req.getPageNum() != null && req.getPageNum() > 0 ? req.getPageNum() : 1;
        int pageSize = req.getPageSize() != null && req.getPageSize() > 0 ? req.getPageSize() : 20;
        com.github.pagehelper.PageHelper.startPage(pageNum, pageSize);
        List<WorkOrder> list = workOrderManager.list(q);
        long total = (list instanceof com.github.pagehelper.Page)
                ? ((com.github.pagehelper.Page<?>) list).getTotal()
                : list.size();
        fillCustomerCompanyName(list);
        return new QueryListResult(list, total);
    }

    /**
     * 任务列表顶部状态计数：当前 scope + 筛选条件下，按 status 分组统计全量数据。
     * 与 queryList 共用 buildListQuery，且强制忽略 status 入参，保证切换状态 chip 时计数稳定。
     */
    public java.util.Map<String, Long> queryStatusCounts(QueryWorkOrderReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<WorkOrder> q = buildListQuery(req, tenantId, loginUser, false);
        // 仅取 status 列，少传字段；不分页
        q.select(WorkOrder::getStatus);
        List<WorkOrder> list = workOrderManager.list(q);
        return list.stream()
                .filter(w -> StringUtils.isNotBlank(w.getStatus()))
                .collect(java.util.stream.Collectors.groupingBy(
                        WorkOrder::getStatus, java.util.stream.Collectors.counting()));
    }

    /**
     * 按入参的 orderByColumn / isAsc 应用排序；默认 create_time DESC。
     * 用白名单过滤列名，避免 SQL 注入；不在白名单的列名直接忽略走默认排序。
     */
    private void applyOrderBy(LambdaQueryWrapper<WorkOrder> q, String column, String isAsc) {
        boolean asc = "asc".equalsIgnoreCase(isAsc);
        switch (column == null ? "" : column) {
            case "createTime":
            case "create_time":
                if (asc) q.orderByAsc(WorkOrder::getCreateTime);
                else     q.orderByDesc(WorkOrder::getCreateTime);
                break;
            default:
                q.orderByDesc(WorkOrder::getCreateTime);
        }
    }

    /**
     * 抽取 queryList / queryStatusCounts 共用的过滤条件。
     * @param applyStatus 是否将 req.status 当作过滤条件（计数接口需要忽略）
     */
    private LambdaQueryWrapper<WorkOrder> buildListQuery(QueryWorkOrderReq req, Long tenantId,
                                                         LoginUser loginUser, boolean applyStatus) {
        LambdaQueryWrapper<WorkOrder> q = new LambdaQueryWrapper<>();
        q.eq(WorkOrder::getTenantId, tenantId);
        if (StringUtils.isNotBlank(req.getTaskType())) q.eq(WorkOrder::getTaskType, req.getTaskType());
        if (applyStatus && StringUtils.isNotBlank(req.getStatus())) q.eq(WorkOrder::getStatus, req.getStatus());
        if (StringUtils.isNotBlank(req.getTaskNo())) q.like(WorkOrder::getTaskNo, req.getTaskNo());
        if (StringUtils.isNotBlank(req.getTitle())) q.like(WorkOrder::getTitle, req.getTitle());
        if (req.getCompanyId() != null) q.eq(WorkOrder::getCompanyId, req.getCompanyId());
        if (req.getCustomerId() != null) q.eq(WorkOrder::getCustomerId, req.getCustomerId());
        // 客户名称模糊查询：先解析到 customerId 列表再过滤；无匹配时强制空集，避免命中全表
        if (StringUtils.isNotBlank(req.getCustomerName())) {
            java.util.List<Long> ids = customerInfoManager.lambdaQuery()
                    .like(CustomerInfo::getCustomerName, req.getCustomerName())
                    .select(CustomerInfo::getId)
                    .list().stream()
                    .map(CustomerInfo::getId)
                    .collect(java.util.stream.Collectors.toList());
            if (ids.isEmpty()) ids = java.util.Collections.singletonList(-1L);
            q.in(WorkOrder::getCustomerId, ids);
        }
        // 公司名称模糊查询：同上
        if (StringUtils.isNotBlank(req.getCompanyName())) {
            java.util.List<Long> ids = customerCompanyManager.lambdaQuery()
                    .like(CustomerCompany::getCompanyName, req.getCompanyName())
                    .select(CustomerCompany::getId)
                    .list().stream()
                    .map(CustomerCompany::getId)
                    .collect(java.util.stream.Collectors.toList());
            if (ids.isEmpty()) ids = java.util.Collections.singletonList(-1L);
            q.in(WorkOrder::getCompanyId, ids);
        }
        if (StringUtils.isNotBlank(req.getCurrentOwnerUser())) q.eq(WorkOrder::getCurrentOwnerUser, req.getCurrentOwnerUser());
        if (StringUtils.isNotBlank(req.getOwnerUser())) q.eq(WorkOrder::getOwnerUser, req.getOwnerUser());
        // 「我的全部」：当前持有人 = 我 或 发起人 = 我
        if (StringUtils.isNotBlank(req.getMineUser())) {
            String me = req.getMineUser();
            q.and(w -> w.eq(WorkOrder::getCurrentOwnerUser, me).or().eq(WorkOrder::getOwnerUser, me));
        }
        // 数据权限：未指定具体用户筛选时，按 loginUser 数据范围过滤
        boolean noUserFilter = StringUtils.isBlank(req.getMineUser())
                && StringUtils.isBlank(req.getOwnerUser())
                && StringUtils.isBlank(req.getCurrentOwnerUser());
        if (noUserFilter && loginUser != null && !loginUser.isAdmin()) {
            java.util.List<Long> scopeUserIds = loginUser.getDataScopeUserList();
            if (scopeUserIds != null) {
                java.util.List<String> usernames;
                if (scopeUserIds.isEmpty()) {
                    usernames = java.util.Collections.singletonList(loginUser.getUsername());
                } else {
                    usernames = userManagerForScope.lambdaQuery()
                            .in(com.qykj.finmaster.system.user.entity.User::getId, scopeUserIds)
                            .list().stream()
                            .map(com.qykj.finmaster.system.user.entity.User::getUsername)
                            .filter(StringUtils::isNotBlank)
                            .collect(java.util.stream.Collectors.toList());
                    if (usernames.isEmpty()) usernames = java.util.Collections.singletonList(loginUser.getUsername());
                }
                final java.util.List<String> finalNames = usernames;
                q.and(w -> w.in(WorkOrder::getOwnerUser, finalNames)
                        .or().in(WorkOrder::getCurrentOwnerUser, finalNames));
            }
        }
        if (Boolean.TRUE.equals(req.getTopLevelOnly())) q.isNull(WorkOrder::getParentTaskId);
        return q;
    }

    /* ==================== 私有工具 ==================== */

    private WorkOrder getAndCheck(Long id) {
        WorkOrder wo = workOrderManager.getById(id);
        if (wo == null) throw new BusinessException("任务单不存在");
        return wo;
    }

    /**
     * 守卫：WAITING_FIX 状态（财务跟进单等待管家修正）期间禁止任何用户操作。
     * 该状态由会计驳回 FINANCE_FOLLOW_UP 进入，仅由管家二次完成 HANDYMAN_FILL 节点的内部路径
     * 通过 reactivateFollowUp() 转出，不允许其它路径修改。
     */
    private void assertNotWaitingFix(WorkOrder wo) {
        if (wo != null && WorkOrderStatus.WAITING_FIX.name().equals(wo.getStatus())) {
            throw new BusinessException("该任务单等待管家修正中，无法操作。请等管家在源工单完成修正后再处理。");
        }
    }

    /** 公开版本，供 Controller 在直接操作 mapper 的场景（如附件上传）使用 */
    public void assertNotWaitingFixById(Long workOrderId) {
        if (workOrderId == null) return;
        WorkOrder wo = workOrderManager.getById(workOrderId);
        assertNotWaitingFix(wo);
    }

    /**
     * 找到指定老工单（HANDYMAN_FILL 所在的 BOOKKEEPING 工单）派生的 FINANCE_FOLLOW_UP 财务跟进单。
     * 通过 sourceType=PARENT + sourceId + subType=FINANCE_FOLLOW_UP 三元组定位。
     * 返回 null 表示尚未派生（首次完成路径）。
     */
    private WorkOrder findFinanceFollowUp(Long oldWorkOrderId) {
        if (oldWorkOrderId == null) return null;
        return workOrderManager.lambdaQuery()
                .eq(WorkOrder::getSourceType, WorkOrderSourceType.PARENT.name())
                .eq(WorkOrder::getSourceId, oldWorkOrderId)
                .eq(WorkOrder::getSubType, "FINANCE_FOLLOW_UP")
                .last("LIMIT 1")
                .one();
    }

    private WorkOrderTaskType parseType(String code) {
        WorkOrderTaskType t = WorkOrderTaskType.of(code);
        if (t == null) throw new BusinessException("无效的任务类型：" + code);
        return t;
    }

    private WorkOrderStep getCurrentStep(Long workOrderId) {
        return stepManager.lambdaQuery()
                .eq(WorkOrderStep::getWorkOrderId, workOrderId)
                .eq(WorkOrderStep::getIsCurrent, 1)
                .one();
    }

    private WorkOrderStep getNextStep(Long workOrderId, Integer currentSort) {
        return stepManager.lambdaQuery()
                .eq(WorkOrderStep::getWorkOrderId, workOrderId)
                .gt(WorkOrderStep::getSort, currentSort)
                .eq(WorkOrderStep::getStatus, WorkOrderStepStatus.PENDING.name())
                .orderByAsc(WorkOrderStep::getSort)
                .last("limit 1")
                .one();
    }

    private WorkOrderStep getPrevStep(Long workOrderId, Integer currentSort) {
        return stepManager.lambdaQuery()
                .eq(WorkOrderStep::getWorkOrderId, workOrderId)
                .lt(WorkOrderStep::getSort, currentSort)
                .orderByDesc(WorkOrderStep::getSort)
                .last("limit 1")
                .one();
    }

    private boolean allChildrenDone(Long parentId) {
        List<WorkOrder> children = listChildren(parentId);
        if (children == null || children.isEmpty()) return true;
        for (WorkOrder c : children) {
            if (!WorkOrderStatus.DONE.name().equals(c.getStatus())
                    && !WorkOrderStatus.CANCELED.name().equals(c.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private void notifyParentIfAllChildDone(WorkOrder child, LoginUser loginUser) {
        if (child.getParentTaskId() == null) return;
        WorkOrder parent = workOrderManager.getById(child.getParentTaskId());
        if (parent == null) return;
        if (!WorkOrderStatus.WAITING_CHILD.name().equals(parent.getStatus())) return;
        if (!allChildrenDone(parent.getId())) return;
        // 父任务回到 RUNNING 状态，允许发起人/当前持有人点完成
        String prev = parent.getStatus();
        parent.setStatus(WorkOrderStatus.RUNNING.name());
        parent.setUpdateTime(new Date());
        workOrderManager.updateById(parent);
        writeLog(parent.getId(), null, WorkOrderAction.EXECUTE,
                prev, WorkOrderStatus.RUNNING.name(),
                null, null, "所有子任务已完成，父任务可以完成", loginUser, parent.getTenantId());
    }

    private void writeLog(Long workOrderId, Long stepId, WorkOrderAction action,
                          String fromStatus, String toStatus,
                          String fromOwner, String toOwner,
                          String content, LoginUser loginUser, Long tenantId) {
        WorkOrderLog lg = new WorkOrderLog();
        lg.setTenantId(tenantId);
        lg.setWorkOrderId(workOrderId);
        lg.setStepId(stepId);
        lg.setAction(action.name());
        lg.setFromStatus(fromStatus);
        lg.setToStatus(toStatus);
        lg.setFromOwnerUser(fromOwner);
        lg.setToOwnerUser(toOwner);
        lg.setContent(content);
        if (loginUser != null) {
            lg.setOperator(loginUser.getUsername());
            lg.setOperatorName(loginUser.getRealName());
        }
        lg.setCreateTime(new Date());
        logManager.save(lg);
    }

    /**
     * 续费任务完成 → 把工单 account_set_expiry_date 推给财务系统更新账套截止日期。
     * 数据源直接来自 work_order.account_set_expiry_date（前端在续费完成弹窗里填写，已持久化）。
     */
    private void syncRenewalExpiryToFinance(WorkOrder wo) {
        if (wo.getCompanyId() == null) return;
        try {
            com.qykj.finmaster.customer.company.domain.CustomerCompany cc =
                    customerCompanyManager.getById(wo.getCompanyId());
            if (cc == null || StringUtils.isBlank(cc.getTaxNumber())) {
                log.info("[续费→财务] 公司或税号为空，跳过 workOrderId={}", wo.getId());
                return;
            }
            if (wo.getAccountSetExpiryDate() == null) {
                log.info("[续费→财务] 工单未填写账套截止日期，跳过 workOrderId={}", wo.getId());
                return;
            }
            String effectiveDate = wo.getAccountSetExpiryDate().toString(); // yyyy-MM-dd
            com.alibaba.fastjson.JSONObject resp =
                    com.qykj.finmaster.common.utils.third.FinancialUtils
                            .updateAccountSetEffectiveDate(cc.getTaxNumber(), effectiveDate);
            com.alibaba.fastjson.JSONObject data = resp != null ? resp.getJSONObject("data") : null;
            if (data != null && Boolean.TRUE.equals(data.getBoolean("success"))) {
                log.info("[续费→财务] 账套截止日期更新成功 taxNumber={}, effectiveDate={}",
                        cc.getTaxNumber(), effectiveDate);
            } else {
                log.error("[续费→财务] 账套截止日期更新失败 taxNumber={}, resp={}", cc.getTaxNumber(), resp);
            }
        } catch (Exception e) {
            log.error("[续费→财务] 账套截止日期同步异常 workOrderId={}, companyId={}",
                    wo.getId(), wo.getCompanyId(), e);
        }
    }

    /**
     * 节点完成后的业务钩子（按 step_code 分派）
     * 失败不影响主流程（在 helper 内部 try/catch）
     */
    private void triggerStepPostActions(WorkOrder wo, WorkOrderStep doneStep, LoginUser loginUser) {
        try {
            String stepCode = doneStep.getStepCode();
            String taskType = wo.getTaskType();
            if (WorkOrderStepCode.FINANCE_RECEIVE.name().equals(stepCode)
                    && WorkOrderTaskType.BOOKKEEPING.name().equals(taskType)) {
                bookkeepingHelper.onFinanceReceived(wo.getCompanyId(), wo, loginUser);
            }
            // 续费任务完成后：续费订单 + 续费合同已在 completeRenewal() 里同事务建好，此处把账套截止日期同步给财务系统。
            if (WorkOrderStepCode.RENEWAL_EXECUTE.name().equals(stepCode)
                    && WorkOrderTaskType.RENEWAL.name().equals(taskType)) {
                syncRenewalExpiryToFinance(wo);
            }
            // ===== 退款流程钩子（同步 refund_order_info 状态位 + 末节点级联） =====
            if (WorkOrderTaskType.REFUND.name().equals(taskType) && wo.getSourceId() != null) {
                refundStepHookSync(wo, doneStep);
            }
            // ===== 服务转移流程钩子（K=1 合同驱动版，末节点直接改公司服务 + TERMINATE 合同） =====
            if (WorkOrderTaskType.SERVICE_TRANSFER.name().equals(taskType) && wo.getSourceId() != null) {
                serviceTransferStepHookSync(wo, doneStep);
            }
            // 其它类型的节点钩子留待后续加
        } catch (Exception e) {
            log.error("[节点钩子] 执行异常 workOrderId={}, stepCode={}", wo.getId(), doneStep.getStepCode(), e);
        }
    }

    /** 任务单号 TASK-yyyyMMdd-XXXX */
    /**
     * 退款工单每步完成时同步 refund_order_info 状态位 + 截图字段
     * REFUND_REVIEW    → refundStatus = PASS
     * REFUND_TRANSFER  → isTransfer = 1（截图取最近一张 fileType=REFUND_TRANSFER 的附件）
     * REFUND_CANCEL_TAX → isCancelTax = 1（截图取最近一张 fileType=REFUND_CANCEL_TAX 的附件）
     * 工单整体 DONE 时把退款 Order 状态切到 REFUNDED（在 executeCurrentStep 末节点已处理）
     */
    private void refundStepHookSync(WorkOrder wo, WorkOrderStep doneStep) {
        Long refundInfoId = wo.getSourceId();
        com.qykj.finmaster.order.domain.RefundOrderInfo info = refundOrderInfoManager.getById(refundInfoId);
        if (info == null) return;
        String stepCode = doneStep.getStepCode();
        // 节点 0：退款审核完成 → refund_order_info.refundStatus = PASS
        if (WorkOrderStepCode.REFUND_REVIEW.name().equals(stepCode)) {
            com.qykj.finmaster.order.domain.RefundOrderInfo upd = new com.qykj.finmaster.order.domain.RefundOrderInfo();
            upd.setId(refundInfoId);
            upd.setStatus("PASS");
            refundOrderInfoManager.updateById(upd);
        }
        // 节点 1：取消办税（仅校验开关是否打开；账套停用动作放到末节点 REFUND_TRANSFER）
        else if (WorkOrderStepCode.REFUND_CANCEL_TAX.name().equals(stepCode)) {
            if (info.getIsCancelTax() == null || info.getIsCancelTax() != 1) {
                throw new BusinessException("取消办税开关未打开，请先在表单中切换为「是」再完成本节点");
            }
            // 财务系统账套停用 / 订单状态 / 公司服务级联，全部在 REFUND_TRANSFER 节点完成时统一执行，
            // 这样钱实际打出去后再彻底停账套，避免中途回滚导致状态错乱
        }
        // 节点 2：退款转账（实际转账，截图上传）
        else if (WorkOrderStepCode.REFUND_TRANSFER.name().equals(stepCode)) {
            String img = latestAttachmentUrl(wo.getId(), "REFUND_TRANSFER");
            com.qykj.finmaster.order.domain.RefundOrderInfo upd = new com.qykj.finmaster.order.domain.RefundOrderInfo();
            upd.setId(refundInfoId);
            upd.setIsTransfer(1);
            if (img != null) upd.setTransferImg(img);
            // 财务打款是末节点：把退款订单 status 切到已退款
            refundOrderInfoManager.updateById(upd);
            // 财务系统：转账完成时按公司税号停用账套（钱已实际打出，账套可彻底关停）
            // 多重兜底解析税号：wo.companyId → refund_order_info.companyId → 原销售订单 customer 公司
            try {
                String taxNumber = resolveTaxNumberForRefund(wo, info);
                if (StringUtils.isNotBlank(taxNumber)) {
                    com.alibaba.fastjson.JSONObject resp =
                            com.qykj.finmaster.common.utils.third.FinancialUtils
                                    .deactivateAccountSet(taxNumber);
                    com.alibaba.fastjson.JSONObject data = resp != null ? resp.getJSONObject("data") : null;
                    if (data != null && Boolean.TRUE.equals(data.getBoolean("success"))) {
                        log.info("[退款·转账→财务] 账套已停用 taxNumber={}", taxNumber);
                    } else {
                        log.error("[退款·转账→财务] 账套停用失败 taxNumber={}, resp={}", taxNumber, resp);
                    }
                } else {
                    log.warn("[退款·转账→财务] 无法解析公司税号，跳过停用账套 workOrderId={}, companyId={}",
                            wo.getId(), wo.getCompanyId());
                }
            } catch (Exception e) {
                log.error("[退款·转账→财务] 账套停用接口异常 workOrderId={}, companyId={}",
                        wo.getId(), wo.getCompanyId(), e);
            }
            try {
                if (wo.getOrderId() != null) {
                    // 1) 退款订单自身（type=REFUND）→ REFUNDED
                    renewalOrderManager.lambdaUpdate()
                            .set(com.qykj.finmaster.order.domain.Order::getStatus,
                                 com.qykj.finmaster.common.core.domain.constansts.DictConstants.ORDER_STATUS_REFUNDED)
                            .eq(com.qykj.finmaster.order.domain.Order::getId, wo.getOrderId())
                            .update();
                    // 2) 原订单（按 sourceOrderNo 反查）也置为 REFUNDED；同时关联合同标记为已终止
                    com.qykj.finmaster.order.domain.Order refundOrder = renewalOrderManager.getById(wo.getOrderId());
                    String sourceOrderNo = refundOrder != null ? refundOrder.getSourceOrderNo() : null;
                    if (StringUtils.isNotBlank(sourceOrderNo)) {
                        renewalOrderManager.lambdaUpdate()
                                .set(com.qykj.finmaster.order.domain.Order::getStatus,
                                     com.qykj.finmaster.common.core.domain.constansts.DictConstants.ORDER_STATUS_REFUNDED)
                                .eq(com.qykj.finmaster.order.domain.Order::getOrderNo, sourceOrderNo)
                                .update();
                        // 3) 关联合同：原订单的明细在 contract_detail 中关联到合同 → 把这些合同切到 TERMINATED
                        java.util.List<com.qykj.finmaster.contract.domain.ContractDetail> details =
                                renewalContractDetailManager.lambdaQuery()
                                        .eq(com.qykj.finmaster.contract.domain.ContractDetail::getOrderNo, sourceOrderNo)
                                        .list();
                        java.util.Set<Long> contractIds = details.stream()
                                .map(com.qykj.finmaster.contract.domain.ContractDetail::getContractId)
                                .filter(java.util.Objects::nonNull)
                                .collect(java.util.stream.Collectors.toSet());
                        if (!contractIds.isEmpty()) {
                            contractManager.lambdaUpdate()
                                    .set(Contract::getStatus,
                                         com.qykj.finmaster.common.core.domain.constansts.DictConstants.CONTRACT_STATUS_TERMINATED)
                                    .in(Contract::getId, contractIds)
                                    .update();
                            // 4) 公司服务：按 source_id IN contractIds 标记为合作取消
                            try {
                                companyServiceManager.lambdaUpdate()
                                        .set(com.qykj.finmaster.customer.company.domain.CompanyService::getStatus, "CANCELLED")
                                        .in(com.qykj.finmaster.customer.company.domain.CompanyService::getSourceId, contractIds)
                                        .update();
                            } catch (Exception ignore) { /* 公司服务取消失败不阻断 */ }
                            // 4.1) 兜底：按"退款订单明细 business_id + 公司 id"标记 company_service（防止 contract→cs 链断了）
                            try {
                                if (wo.getCompanyId() != null) {
                                    java.util.Set<Long> bizIds = orderDetailManager.lambdaQuery()
                                            .eq(com.qykj.finmaster.order.domain.OrderDetail::getOrderId, wo.getOrderId())
                                            .list().stream()
                                            .map(com.qykj.finmaster.order.domain.OrderDetail::getBusinessId)
                                            .filter(java.util.Objects::nonNull)
                                            .collect(java.util.stream.Collectors.toSet());
                                    if (!bizIds.isEmpty()) {
                                        companyServiceManager.lambdaUpdate()
                                                .set(com.qykj.finmaster.customer.company.domain.CompanyService::getStatus, "CANCELLED")
                                                .eq(com.qykj.finmaster.customer.company.domain.CompanyService::getCompanyId, wo.getCompanyId())
                                                .in(com.qykj.finmaster.customer.company.domain.CompanyService::getBusinessId, bizIds)
                                                .update();
                                        log.info("[退款·公司服务兜底] 按 (companyId={}, businessIds={}) 标记 company_service.status=CANCELLED",
                                                wo.getCompanyId(), bizIds);
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("[退款·公司服务兜底] 失败 workOrderId={}", wo.getId(), e);
                            }
                            // 5) 公司整体合作状态保险：若该公司下的所有公司服务都已 CANCELLED，则把 customer_company.company_status 置 CANCELLED
                            try {
                                if (wo.getCompanyId() != null) {
                                    long activeCount = companyServiceManager.lambdaQuery()
                                            .eq(com.qykj.finmaster.customer.company.domain.CompanyService::getCompanyId, wo.getCompanyId())
                                            .ne(com.qykj.finmaster.customer.company.domain.CompanyService::getStatus, "CANCELLED")
                                            .count();
                                    if (activeCount == 0) {
                                        customerCompanyManager.lambdaUpdate()
                                                .set(com.qykj.finmaster.customer.company.domain.CustomerCompany::getCompanyStatus, "CANCELLED")
                                                .eq(com.qykj.finmaster.customer.company.domain.CustomerCompany::getId, wo.getCompanyId())
                                                .update();
                                        log.info("[退款·公司状态级联] 公司 {} 所有公司服务均为 CANCELLED，company_status 置为 CANCELLED",
                                                wo.getCompanyId());
                                    } else {
                                        log.info("[退款·公司状态级联] 公司 {} 仍有 {} 条活跃服务，保持原 company_status",
                                                wo.getCompanyId(), activeCount);
                                    }
                                }
                            } catch (Exception ignore) { /* 公司状态级联失败不阻断 */ }
                        }
                    }
                }
            } catch (Exception ignore) { /* 同步状态失败不阻断工单完成 */ }
        }
    }

    /**
     * SERVICE_TRANSFER 工单（K=1 合同驱动版，2 节点）每步钩子：
     *   - REFUND_REVIEW：仅前端 UX 确认，后端 no-op
     *   - REFUND_CANCEL_TAX（末节点）：执行实际转移
     *       1) 源 company_service.status = CANCELLED
     *       2) 该 (source_company_id, source_business_id) 下所有 active 合同 → TERMINATED
     *       3) 目标 company_service.actualEndDate = 用户填的 target_actual_end_date
     *       4) 写 company_service_renew 行（renewType=0，renewAmount=0）
     *       5) 财务系统：源公司账套停用 / 目标公司账套截止延期
     *       6) 回填 service_transfer_item.process_status = DONE
     */
    private void serviceTransferStepHookSync(WorkOrder wo, WorkOrderStep doneStep) {
        String stepCode = doneStep.getStepCode();
        // 审核节点无后端动作；只在末节点 SERVICE_TRANSFER_CANCEL_TAX 触发实际转移
        if (!WorkOrderStepCode.SERVICE_TRANSFER_CANCEL_TAX.name().equals(stepCode)) return;

        // 末节点：取 service_transfer_item（K=1，一个工单只有一条）
        com.qykj.finmaster.order.domain.ServiceTransferItem it = serviceTransferItemManager.lambdaQuery()
                .eq(com.qykj.finmaster.order.domain.ServiceTransferItem::getWorkOrderId, wo.getId())
                .last("LIMIT 1")
                .one();
        if (it == null) {
            log.error("[服务转移] workOrderId={} 找不到 service_transfer_item，跳过末节点动作", wo.getId());
            return;
        }
        if ("DONE".equals(it.getProcessStatus())) {
            log.info("[服务转移] item={} 已 DONE，跳过重复处理", it.getId());
            return;
        }
        Date now = new Date();
        try {
            executeServiceTransfer(wo, it, now);
            serviceTransferItemManager.lambdaUpdate()
                    .eq(com.qykj.finmaster.order.domain.ServiceTransferItem::getId, it.getId())
                    .set(com.qykj.finmaster.order.domain.ServiceTransferItem::getProcessStatus, "DONE")
                    .set(com.qykj.finmaster.order.domain.ServiceTransferItem::getProcessRemark, null)
                    .set(com.qykj.finmaster.order.domain.ServiceTransferItem::getUpdateTime, now)
                    .update();
            log.info("[服务转移] item={} 末节点处理完成 (workOrderId={})", it.getId(), wo.getId());
        } catch (Exception e) {
            log.error("[服务转移] item={} 末节点处理失败 (workOrderId={})", it.getId(), wo.getId(), e);
            String remark = StringUtils.left("失败：" + e.getMessage(), 500);
            serviceTransferItemManager.lambdaUpdate()
                    .eq(com.qykj.finmaster.order.domain.ServiceTransferItem::getId, it.getId())
                    .set(com.qykj.finmaster.order.domain.ServiceTransferItem::getProcessStatus, "FAILED")
                    .set(com.qykj.finmaster.order.domain.ServiceTransferItem::getProcessRemark, remark)
                    .set(com.qykj.finmaster.order.domain.ServiceTransferItem::getUpdateTime, now)
                    .update();
            // 抛出让用户看到错误（K=1，没有"其它 item 继续"的需求）
            throw new BusinessException("服务转移执行失败：" + e.getMessage());
        }
    }

    /** 末节点的真正动作。 */
    private void executeServiceTransfer(WorkOrder wo,
                                        com.qykj.finmaster.order.domain.ServiceTransferItem it,
                                        Date now) {
        if (it.getTargetActualEndDate() == null) {
            throw new BusinessException("target_actual_end_date 为空，无法顺延目标服务到期日");
        }

        // 1) 源 company_service → CANCELLED；同时追加 remark 标记本次转移
        com.qykj.finmaster.customer.company.domain.CompanyService srcSvc =
                companyServiceManager.getById(it.getSourceCompanyServiceId());
        if (srcSvc == null) throw new BusinessException("源 company_service 不存在");
        String srcServiceRemarkLine = String.format("[服务转移] %s 工单 %s 转移至公司 #%d，源服务 CANCELLED",
                new java.text.SimpleDateFormat("yyyy-MM-dd").format(now),
                wo.getTaskNo(), it.getTargetCompanyId());
        String srcServiceNewRemark = appendRemarkLine(srcSvc.getRemark(), srcServiceRemarkLine);
        companyServiceManager.lambdaUpdate()
                .eq(com.qykj.finmaster.customer.company.domain.CompanyService::getId, srcSvc.getId())
                .set(com.qykj.finmaster.customer.company.domain.CompanyService::getStatus, "CANCELLED")
                .set(com.qykj.finmaster.customer.company.domain.CompanyService::getRemark, srcServiceNewRemark)
                .set(com.qykj.finmaster.customer.company.domain.CompanyService::getUpdateTime, now)
                .update();

        // 1.1) 公司整体合作状态级联：若源公司下所有公司服务都已 CANCELLED，则 customer_company.company_status = CANCELLED
        try {
            long activeCount = companyServiceManager.lambdaQuery()
                    .eq(com.qykj.finmaster.customer.company.domain.CompanyService::getCompanyId, it.getSourceCompanyId())
                    .ne(com.qykj.finmaster.customer.company.domain.CompanyService::getStatus, "CANCELLED")
                    .count();
            if (activeCount == 0) {
                customerCompanyManager.lambdaUpdate()
                        .eq(com.qykj.finmaster.customer.company.domain.CustomerCompany::getId, it.getSourceCompanyId())
                        .set(com.qykj.finmaster.customer.company.domain.CustomerCompany::getCompanyStatus, "CANCELLED")
                        .set(com.qykj.finmaster.customer.company.domain.CustomerCompany::getUpdateTime, now)
                        .update();
                log.info("[服务转移·公司状态级联] 源公司 {} 所有公司服务均为 CANCELLED，company_status 置为 CANCELLED",
                        it.getSourceCompanyId());
            } else {
                log.info("[服务转移·公司状态级联] 源公司 {} 仍有 {} 条活跃服务，保持原合作状态",
                        it.getSourceCompanyId(), activeCount);
            }
        } catch (Exception e) {
            // 公司状态级联失败不阻断主流程，仅 log
            log.warn("[服务转移·公司状态级联] 失败 sourceCompanyId={}", it.getSourceCompanyId(), e);
        }

        // 2) 该 (sourceCompanyId, sourceBusinessId) 下所有 active 合同 → TERMINATED
        contractManager.lambdaUpdate()
                .eq(Contract::getCompanyId, it.getSourceCompanyId())
                .eq(Contract::getBusinessId, it.getSourceBusinessId())
                .notIn(Contract::getStatus, java.util.Arrays.asList(
                        com.qykj.finmaster.common.core.domain.constansts.DictConstants.CONTRACT_STATUS_TERMINATED,
                        com.qykj.finmaster.common.core.domain.constansts.DictConstants.CONTRACT_STATUS_DISUSED))
                .set(Contract::getStatus, com.qykj.finmaster.common.core.domain.constansts.DictConstants.CONTRACT_STATUS_TERMINATED)
                .set(Contract::getUpdateTime, now)
                .update();

        // 3) 目标 company_service.actualEndDate = 用户填写的新到期日
        com.qykj.finmaster.customer.company.domain.CompanyService tgtSvc =
                companyServiceManager.getById(it.getTargetCompanyServiceId());
        if (tgtSvc == null) throw new BusinessException("目标 company_service 不存在");
        java.time.LocalDate prevEnd = tgtSvc.getActualEndDate();
        companyServiceManager.lambdaUpdate()
                .eq(com.qykj.finmaster.customer.company.domain.CompanyService::getId, tgtSvc.getId())
                .set(com.qykj.finmaster.customer.company.domain.CompanyService::getActualEndDate, it.getTargetActualEndDate())
                .set(com.qykj.finmaster.customer.company.domain.CompanyService::getUpdateTime, now)
                .update();

        // 3.1) 找到目标公司同业务下"最新"的一份合同（contract_end_time DESC），延长其 contract_end_time + 追加 remark
        //      找不到时仅 log warning，不阻断（company_service 已改对，只是少了合同审计痕迹）
        try {
            Contract latestTgtContract = contractManager.lambdaQuery()
                    .eq(Contract::getCompanyId, it.getTargetCompanyId())
                    .eq(Contract::getBusinessId, it.getSourceBusinessId())   // 同业务（源/目标必相同）
                    .notIn(Contract::getStatus, java.util.Arrays.asList(
                            com.qykj.finmaster.common.core.domain.constansts.DictConstants.CONTRACT_STATUS_TERMINATED,
                            com.qykj.finmaster.common.core.domain.constansts.DictConstants.CONTRACT_STATUS_DISUSED))
                    .orderByDesc(Contract::getContractEndTime)
                    .last("LIMIT 1")
                    .one();
            if (latestTgtContract != null) {
                String ctRemarkLine = String.format("[服务转移] %s 工单 %s 由公司 #%d 转入，结束日期延至 %s",
                        new java.text.SimpleDateFormat("yyyy-MM-dd").format(now),
                        wo.getTaskNo(), it.getSourceCompanyId(), it.getTargetActualEndDate());
                String ctNewRemark = appendRemarkLine(latestTgtContract.getRemark(), ctRemarkLine);
                contractManager.lambdaUpdate()
                        .eq(Contract::getId, latestTgtContract.getId())
                        .set(Contract::getContractEndTime, java.sql.Date.valueOf(it.getTargetActualEndDate()))
                        .set(Contract::getRemark, ctNewRemark)
                        .set(Contract::getUpdateTime, now)
                        .update();
                log.info("[服务转移·目标合同延期] contractId={} contractNo={} end_time → {}",
                        latestTgtContract.getId(), latestTgtContract.getContractNo(), it.getTargetActualEndDate());
            } else {
                log.warn("[服务转移·目标合同延期] 目标公司 {} 业务 {} 下无活跃合同可延期，仅服务到期日已更新",
                        it.getTargetCompanyId(), it.getSourceBusinessId());
            }
        } catch (Exception e) {
            log.warn("[服务转移·目标合同延期] 失败 itemId={}", it.getId(), e);
        }

        // 4) 写 company_service_renew 行（renewType=0 正常续期，renewAmount=0）
        try {
            java.time.LocalDate renewStart = prevEnd != null ? prevEnd : java.time.LocalDate.now();
            com.qykj.finmaster.customer.company.domain.req.AddCompanyRenewReq renewReq =
                    new com.qykj.finmaster.customer.company.domain.req.AddCompanyRenewReq();
            renewReq.setCompanyServiceId(it.getTargetCompanyServiceId());
            renewReq.setRenewEffectiveDate(renewStart);
            renewReq.setRenewEndDate(it.getTargetActualEndDate());
            renewReq.setRenewType(0);
            renewReq.setRenewAmount(java.math.BigDecimal.ZERO);
            renewReq.setRemark("由服务转移工单 " + wo.getTaskNo() + " 自动生成（来源合同 "
                    + wo.getSourceNumber() + "）");
            companyServiceService.addCompanyRenew(renewReq, null);
        } catch (Exception e) {
            // addCompanyRenew 内部已经把 actualEndDate 改成 renewEnd 了；
            // 上面第 3 步又强制改回 target_actual_end_date，所以即使 renew 写失败也不影响目标服务的到期日。
            // 这里只记录，不阻断（renew 行只是审计/历史）
            log.warn("[服务转移] 写 company_service_renew 失败 itemId={}，目标服务到期日已直接更新", it.getId(), e);
        }

        // 5) 财务系统：源公司账套停用 + 目标公司账套截止日期延期
        try {
            com.qykj.finmaster.customer.company.domain.CustomerCompany srcCompany =
                    customerCompanyManager.getById(it.getSourceCompanyId());
            if (srcCompany != null && StringUtils.isNotBlank(srcCompany.getTaxNumber())) {
                com.alibaba.fastjson.JSONObject resp =
                        com.qykj.finmaster.common.utils.third.FinancialUtils
                                .deactivateAccountSet(srcCompany.getTaxNumber());
                log.info("[服务转移·财务] 源公司 {} 账套停用 resp={}", srcCompany.getTaxNumber(), resp);
            }
        } catch (Exception e) {
            log.warn("[服务转移·财务] 源公司账套停用失败 itemId={}", it.getId(), e);
        }
        try {
            com.qykj.finmaster.customer.company.domain.CustomerCompany tgtCompany =
                    customerCompanyManager.getById(it.getTargetCompanyId());
            if (tgtCompany != null && StringUtils.isNotBlank(tgtCompany.getTaxNumber())) {
                java.time.LocalDate newExpiry = it.getTargetActualEndDate()
                        .with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
                com.alibaba.fastjson.JSONObject resp =
                        com.qykj.finmaster.common.utils.third.FinancialUtils
                                .updateAccountSetEffectiveDate(tgtCompany.getTaxNumber(), newExpiry.toString());
                log.info("[服务转移·财务] 目标公司 {} 账套截止延期到 {} resp={}",
                        tgtCompany.getTaxNumber(), newExpiry, resp);
            }
        } catch (Exception e) {
            log.warn("[服务转移·财务] 目标公司账套延期失败 itemId={}", it.getId(), e);
        }
    }

    /** 在已有 remark 末尾追加一行（旧值为空时直接返回 line；截断到 500 字符避免溢出 VARCHAR(500)） */
    private String appendRemarkLine(String existing, String line) {
        String combined = (existing == null || existing.isEmpty()) ? line : (existing + "\n" + line);
        return combined.length() > 500 ? combined.substring(combined.length() - 500) : combined;
    }

    /**
     * 退款工单兜底解析公司税号：
     *   1) wo.companyId → customer_company.tax_number
     *   2) wo.orderId → orders.company_id（退款订单本身的 companyId，可能比 wo 上的更准）
     *   3) 退款订单的 sourceOrderNo → 原销售订单 → companyId
     *   4) 退款订单 customerId → customer_company（按客户取第一家有税号的公司）
     *   都查不到返回 null
     */
    private String resolveTaxNumberForRefund(WorkOrder wo, com.qykj.finmaster.order.domain.RefundOrderInfo info) {
        // ① 工单 companyId
        if (wo.getCompanyId() != null) {
            com.qykj.finmaster.customer.company.domain.CustomerCompany cc =
                    customerCompanyManager.getById(wo.getCompanyId());
            if (cc != null && StringUtils.isNotBlank(cc.getTaxNumber())) return cc.getTaxNumber();
        }
        // ② / ③ / ④ 通过退款订单 orderId 反查
        if (wo.getOrderId() != null) {
            com.qykj.finmaster.order.domain.Order refundOrder = renewalOrderManager.getById(wo.getOrderId());
            if (refundOrder != null) {
                // ② 退款订单自身的 companyId
                if (refundOrder.getCompanyId() != null) {
                    com.qykj.finmaster.customer.company.domain.CustomerCompany cc =
                            customerCompanyManager.getById(refundOrder.getCompanyId());
                    if (cc != null && StringUtils.isNotBlank(cc.getTaxNumber())) return cc.getTaxNumber();
                }
                // ③ 原销售订单的 companyId
                if (StringUtils.isNotBlank(refundOrder.getSourceOrderNo())) {
                    com.qykj.finmaster.order.domain.Order srcOrder = renewalOrderManager.lambdaQuery()
                            .eq(com.qykj.finmaster.order.domain.Order::getOrderNo, refundOrder.getSourceOrderNo())
                            .last("LIMIT 1")
                            .one();
                    if (srcOrder != null && srcOrder.getCompanyId() != null) {
                        com.qykj.finmaster.customer.company.domain.CustomerCompany cc =
                                customerCompanyManager.getById(srcOrder.getCompanyId());
                        if (cc != null && StringUtils.isNotBlank(cc.getTaxNumber())) return cc.getTaxNumber();
                    }
                }
                // ④ 客户名下任一家有税号的公司
                if (refundOrder.getCustomerId() != null) {
                    com.qykj.finmaster.customer.company.domain.CustomerCompany cc = customerCompanyManager.lambdaQuery()
                            .eq(com.qykj.finmaster.customer.company.domain.CustomerCompany::getCustomerId, refundOrder.getCustomerId())
                            .isNotNull(com.qykj.finmaster.customer.company.domain.CustomerCompany::getTaxNumber)
                            .last("LIMIT 1")
                            .one();
                    if (cc != null && StringUtils.isNotBlank(cc.getTaxNumber())) return cc.getTaxNumber();
                }
            }
        }
        return null;
    }

    private String latestAttachmentUrl(Long workOrderId, String fileType) {
        WorkOrderAttachment a = attachmentManager.lambdaQuery()
                .eq(WorkOrderAttachment::getWorkOrderId, workOrderId)
                .eq(WorkOrderAttachment::getFileType, fileType)
                .orderByDesc(WorkOrderAttachment::getId)
                .last("LIMIT 1")
                .one();
        return a != null ? a.getFileUrl() : null;
    }

    private String generateTaskNo() {
        String yyyymmdd = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        String rand = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
        return "TASK-" + yyyymmdd + "-" + rand;
    }

    /**
     * 合并存量 ArrayList，避免 Collections.emptyList() 在某些 lambda 操作时不可变
     */
    @SuppressWarnings("unused")
    private static <T> List<T> safeList(List<T> list) {
        return list == null ? new ArrayList<>() : list;
    }
}
