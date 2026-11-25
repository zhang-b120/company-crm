package com.qykj.finmaster.workorder.controller;

import cn.hutool.core.io.file.FileNameUtil;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.workorder.domain.WorkOrder;
import com.qykj.finmaster.workorder.domain.WorkOrderAttachment;
import com.qykj.finmaster.workorder.domain.WorkOrderFieldRecord;
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
import com.qykj.finmaster.workorder.manager.WorkOrderAttachmentManager;
import com.qykj.finmaster.workorder.manager.WorkOrderFieldRecordManager;
import com.qykj.finmaster.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * 任务单 V2 Controller
 * 与旧 /commonTask/* 物理隔离
 */
@RestController
@RequestMapping("/work-order")
public class WorkOrderController extends BaseController {

    @Autowired
    private WorkOrderService service;

    @Autowired
    private WorkOrderAttachmentManager attachmentManager;

    @Autowired
    private WorkOrderFieldRecordManager fieldRecordManager;

    /** 创建任务单（顶级） */
    @PostMapping("/create")
    public Result<WorkOrder> create(@RequestBody @Valid CreateWorkOrderReq req) {
        return Result.ok(service.createWorkOrder(req, getLoginUser(), getTenantId()));
    }

    /** 代账派工弹窗预填：公司/法人 + 股东 + 员工 + 税务凭证 */
    @GetMapping("/dispatch/bookkeeping/preload")
    public Result<com.qykj.finmaster.workorder.domain.resp.BookkeepingDispatchPreloadResp> preloadBookkeeping(
            @RequestParam("companyId") Long companyId) {
        return Result.ok(service.preloadBookkeepingDispatch(companyId));
    }

    /** 代账任务详情页：增量保存公司/法人/股东/员工/税务凭证（不创建新任务，只更新数据） */
    @PostMapping("/dispatch/bookkeeping/save")
    public Result<Void> saveBookkeeping(
            @RequestParam("companyId") Long companyId,
            @RequestBody com.qykj.finmaster.workorder.domain.req.BookkeepingDispatchPayload payload) {
        service.saveBookkeepingDispatch(companyId, payload, getLoginUser(), getTenantId());
        return Result.ok();
    }

    /** 标记某区块"核对无误"（持久化到 work_order.verified_sections） */
    @PostMapping("/{id}/verify")
    public Result<Void> verifySection(
            @PathVariable("id") Long workOrderId,
            @RequestParam("section") String section,
            @RequestParam(value = "verified", defaultValue = "true") Boolean verified) {
        service.markSectionVerified(workOrderId, section, verified, getLoginUser());
        return Result.ok();
    }

    /** 保存期初数据：type 1-无期初 2-电子税务局同步 3-科目余额表 */
    @PostMapping("/{id}/opening-data")
    public Result<Void> saveOpeningData(
            @PathVariable("id") Long workOrderId,
            @RequestParam("type") Integer type,
            @RequestParam(value = "remark", required = false) String remark) {
        service.saveOpeningData(workOrderId, type, remark, getLoginUser());
        return Result.ok();
    }

    /** 保存账套启用日期 / 截止日期（代账任务专用，建账套时使用） */
    @PostMapping("/{id}/account-set-dates")
    public Result<Void> saveAccountSetDates(
            @PathVariable("id") Long workOrderId,
            @RequestParam(value = "enableDate", required = false) String enableDate,
            @RequestParam(value = "expiryDate", required = false) String expiryDate) {
        service.saveAccountSetDates(workOrderId, enableDate, expiryDate, getLoginUser());
        return Result.ok();
    }

    /** 拆子任务（dispatch=false 则保存为草稿，需手动下发） */
    @PostMapping("/split")
    public Result<WorkOrder> split(@RequestBody @Valid SplitSubTaskReq req) {
        return Result.ok(service.splitSubTask(req, getLoginUser(), getTenantId()));
    }

    /**
     * 按 taskType + stepCode 统计各候选用户当前未完成的任务数（DRAFT/PENDING/RUNNING/WAITING_CHILD/SUSPENDED 都算）
     * 返回 Map<username, pendingCount>，前端可在指派下拉里展示并按未完成数升序排列
     */
    @GetMapping("/pending-count-by-user")
    public Result<java.util.Map<String, Long>> pendingCountByUser(
            @RequestParam("taskType") String taskType,
            @RequestParam(value = "stepCode", required = false) String stepCode) {
        return Result.ok(service.countPendingByUser(taskType, stepCode, getTenantId()));
    }

    /**
     * 按 taskType + stepCode 统计各候选用户已完成（status=DONE）的任务数。
     * 返回 Map<username, doneCount>，与 pending-count-by-user 并列，前端展示「未完成/已完成 X/Y」。
     * 注意：只统 DONE，不含 CONCESSION_DONE（让步完成仍归"未完成"侧），与未完成口径互补、不重不漏。
     */
    @GetMapping("/done-count-by-user")
    public Result<java.util.Map<String, Long>> doneCountByUser(
            @RequestParam("taskType") String taskType,
            @RequestParam(value = "stepCode", required = false) String stepCode) {
        return Result.ok(service.countDoneByUser(taskType, stepCode, getTenantId()));
    }

    /** 让步项补齐：财务后期拿到密码 / 缺失资料后，标记让步已解决 */
    @PostMapping("/{id}/resolve-concession")
    public Result<Void> resolveConcession(
            @PathVariable("id") Long workOrderId,
            @RequestParam(value = "remark", required = false) String remark) {
        service.resolveConcession(workOrderId, remark, getLoginUser());
        return Result.ok();
    }

    /** 续费任务详情专用：绕过数据权限，按 companyId 直查公司服务 / 订单 / 合同 */
    @org.springframework.beans.factory.annotation.Autowired
    private com.qykj.finmaster.customer.company.service.CompanyServiceServiceImpl companyServiceService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.qykj.finmaster.order.service.OrderServiceImpl orderService;

    @org.springframework.beans.factory.annotation.Autowired
    private com.qykj.finmaster.contract.service.ContractServiceImpl contractService;

    /** 续费任务详情：按 companyId 查公司全部服务（CANCELLED 也展示，不分页，不过滤 dataScope） */
    @GetMapping("/renewal/services-by-company")
    public Result<java.util.List<com.qykj.finmaster.customer.company.domain.resp.GetCompanyRenewListWithServiceResp>>
            renewalServicesByCompany(@RequestParam("companyId") Long companyId) {
        return Result.ok(companyServiceService.listAllServicesByCompanyIdForRenewal(companyId));
    }

    /**
     * 续费任务列表：按 companyServiceId 查该公司服务下所有续费任务（含 CANCELED；按 id desc）。
     * 用于公司续费列表 "查看续费任务列表" 按钮 —— 一个服务允许多次续费，需要看全部历史任务。
     */
    @GetMapping("/renewal/by-company-service")
    public Result<java.util.List<WorkOrder>> renewalByCompanyService(
            @RequestParam("companyServiceId") Long companyServiceId) {
        if (companyServiceId == null) return Result.ok(java.util.Collections.emptyList());
        return Result.ok(service.listRenewalByCompanyService(companyServiceId));
    }

    /** 续费任务详情：按 companyId 查公司全部非已取消订单（含 orderDetails / paymentTypeName 等富字段，不过滤 dataScope） */
    @GetMapping("/renewal/orders-by-company")
    public Result<java.util.List<com.qykj.finmaster.order.domain.resp.GetOrderListResp>>
            renewalOrdersByCompany(@RequestParam("companyId") Long companyId) {
        if (companyId == null) return Result.ok(java.util.Collections.emptyList());
        com.qykj.finmaster.order.domain.req.GetOrderListInfoReq req =
                new com.qykj.finmaster.order.domain.req.GetOrderListInfoReq();
        req.setCompanyId(companyId);
        java.util.List<com.qykj.finmaster.order.domain.resp.GetOrderListResp> list =
                new java.util.ArrayList<>(orderService.selectOrderListBypassDataScope(req, getTenantId()));
        // 过滤掉已取消的订单
        list.removeIf(o -> com.qykj.finmaster.common.core.domain.constansts.DictConstants
                .ORDER_STATUS_CANCELLED.equalsIgnoreCase(o.getStatus()));
        return Result.ok(list);
    }

    /** 续费任务详情：按 companyId 查公司全部非作废合同（含 businessName 等富字段，不过滤 dataScope） */
    @GetMapping("/renewal/contracts-by-company")
    public Result<java.util.List<com.qykj.finmaster.contract.domain.resp.GetContractListResp>>
            renewalContractsByCompany(@RequestParam("companyId") Long companyId) {
        if (companyId == null) return Result.ok(java.util.Collections.emptyList());
        com.qykj.finmaster.contract.domain.req.GetContractListReq req =
                new com.qykj.finmaster.contract.domain.req.GetContractListReq();
        req.setCompanyId(companyId);
        java.util.List<com.qykj.finmaster.contract.domain.resp.GetContractListResp> list =
                new java.util.ArrayList<>(contractService.getContractListBypassDataScope(req, getTenantId()));
        // 过滤掉作废状态合同
        list.removeIf(c -> com.qykj.finmaster.common.core.domain.constansts.DictConstants
                .CONTRACT_STATUS_DISUSED.equalsIgnoreCase(c.getStatus()));
        return Result.ok(list);
    }

    /* ==================== 数据收集（银行流水） ==================== */

    @org.springframework.beans.factory.annotation.Autowired
    private com.qykj.finmaster.customer.company.service.BankStatementService bankStatementService;

    /** 数据收集：按管家用户名批量下发任务（每人一条工单） */
    @PostMapping("/data-collection/dispatch")
    public Result<List<WorkOrder>> dispatchDataCollection(
            @RequestBody @Valid com.qykj.finmaster.workorder.domain.req.DataCollectionDispatchReq req) {
        return Result.ok(service.batchDispatchDataCollection(req, getLoginUser(), getTenantId()));
    }

    /** 数据收集：列出工单负责管家下属公司及其本期银行流水状态 */
    @GetMapping("/data-collection/{id}/companies")
    public Result<java.util.Map<String, Object>> listDataCollectionCompanies(
            @PathVariable("id") Long workOrderId) {
        return Result.ok(service.listDataCollectionCompanies(workOrderId));
    }

    /** 数据收集：勾选/取消勾选某公司本期"无银行流水" */
    @PostMapping("/data-collection/mark-no-statement")
    public Result<Void> markNoStatement(
            @RequestParam("companyId") Long companyId,
            @RequestParam("period") String period,
            @RequestParam(value = "noStatement", defaultValue = "true") Boolean noStatement) {
        bankStatementService.markNoStatement(companyId, period, Boolean.TRUE.equals(noStatement));
        return Result.ok();
    }

    /** 批量下发续费工单（公司续费列表多选后调用） */
    @PostMapping("/batch-renewal")
    public Result<List<WorkOrder>> batchRenewal(
            @RequestBody @Valid com.qykj.finmaster.workorder.domain.req.BatchDispatchRenewalReq req) {
        return Result.ok(service.batchDispatchRenewal(req, getLoginUser(), getTenantId()));
    }

    /** 续费任务完成：自动建续费订单 + 续费合同，并关闭工单 */
    @PostMapping("/renewal/complete")
    public Result<WorkOrder> completeRenewal(
            @RequestBody @Valid com.qykj.finmaster.workorder.domain.req.CompleteRenewalReq req) {
        return Result.ok(service.completeRenewal(req, getLoginUser(), getTenantId()));
    }

    /** 下发草稿子任务 DRAFT → PENDING */
    @PostMapping("/sub-task/dispatch/{id}")
    public Result<WorkOrder> dispatchSubTask(@PathVariable("id") Long id) {
        return Result.ok(service.dispatchSubTask(id, getLoginUser()));
    }

    /** 修改草稿子任务（前端复用 SplitSubTaskReq，parentTaskId 字段承载子任务 id） */
    @PostMapping("/sub-task/update")
    public Result<WorkOrder> updateSubTaskDraft(@RequestBody SplitSubTaskReq req) {
        return Result.ok(service.updateSubTaskDraft(req, getLoginUser()));
    }

    /** 节点完成（执行） */
    @PostMapping("/execute")
    public Result<WorkOrder> execute(@RequestBody @Valid ExecuteStepReq req) {
        return Result.ok(service.executeCurrentStep(req, getLoginUser()));
    }

    /** 驳回 */
    @PostMapping("/reject")
    public Result<Void> reject(@RequestBody @Valid RejectWorkOrderReq req) {
        service.rejectWorkOrder(req, getLoginUser());
        return Result.ok();
    }

    /** 挂起 */
    @PostMapping("/suspend")
    public Result<Void> suspend(@RequestBody @Valid SuspendWorkOrderReq req) {
        service.suspendWorkOrder(req, getLoginUser());
        return Result.ok();
    }

    /** 恢复 */
    @PostMapping("/resume")
    public Result<Void> resume(@RequestBody @Valid SuspendWorkOrderReq req) {
        service.resumeWorkOrder(req, getLoginUser());
        return Result.ok();
    }

    /** 取消（仅 PENDING + 发起人） */
    @PostMapping("/cancel")
    public Result<Void> cancel(@RequestBody @Valid CancelWorkOrderReq req) {
        service.cancelWorkOrder(req, getLoginUser());
        return Result.ok();
    }

    /** 受理：当前持有人确认接收，PENDING → RUNNING */
    @PostMapping("/accept/{id}")
    public Result<WorkOrder> accept(@PathVariable("id") Long id) {
        return Result.ok(service.acceptWorkOrder(id, getLoginUser()));
    }

    /** 撤回：仅 PENDING 未受理状态可撤回，仅发起人 */
    @PostMapping("/recall/{id}")
    public Result<Void> recall(@PathVariable("id") Long id,
                               @RequestParam(value = "reason", required = false) String reason) {
        service.recallWorkOrder(id, reason, getLoginUser());
        return Result.ok();
    }

    /** 催办 */
    @PostMapping("/urge")
    public Result<Void> urge(@RequestBody @Valid UrgeWorkOrderReq req) {
        service.urgeWorkOrder(req, getLoginUser());
        return Result.ok();
    }

    /** 列表 */
    @PostMapping("/list")
    public Result<List<WorkOrder>> list(@RequestBody QueryWorkOrderReq req) {
        com.qykj.finmaster.workorder.service.WorkOrderService.QueryListResult r =
                service.queryList(req, getTenantId(), getLoginUser());
        return Result.ok(r.getList(), r.getTotal());
    }

    /**
     * 列表顶部状态计数：忽略 req.status，按当前 scope + 其他过滤条件统计每个 status 的总数。
     * 单独走接口避免给主列表加 SELECT GROUP BY 的额外字段，调用频率低。
     */
    @PostMapping("/list/status-counts")
    public Result<java.util.Map<String, Long>> statusCounts(@RequestBody QueryWorkOrderReq req) {
        return Result.ok(service.queryStatusCounts(req, getTenantId(), getLoginUser()));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<WorkOrder> detail(@PathVariable Long id) {
        return Result.ok(service.getDetail(id));
    }

    /** 节点列表 */
    @GetMapping("/{id}/steps")
    public Result<List<WorkOrderStep>> steps(@PathVariable("id") Long workOrderId) {
        return Result.ok(service.listSteps(workOrderId));
    }

    /** 日志时间轴 */
    @GetMapping("/{id}/logs")
    public Result<List<WorkOrderLog>> logs(@PathVariable("id") Long workOrderId) {
        return Result.ok(service.listLogs(workOrderId));
    }

    /** 子任务列表 */
    @GetMapping("/{id}/children")
    public Result<List<WorkOrder>> children(@PathVariable("id") Long parentId) {
        return Result.ok(service.listChildren(parentId));
    }

    /* ==================== 外勤记录（FIELD_WORK 任务专用，时间轴形式） ==================== */

    /** 外勤记录列表（按 createTime 升序，前端时间轴展示） */
    @GetMapping("/{id}/field-records")
    public Result<List<WorkOrderFieldRecord>> listFieldRecords(@PathVariable("id") Long workOrderId) {
        return Result.ok(
                fieldRecordManager.lambdaQuery()
                        .eq(WorkOrderFieldRecord::getWorkOrderId, workOrderId)
                        .orderByAsc(WorkOrderFieldRecord::getCreateTime)
                        .list()
        );
    }

    /** 追加一条外勤记录 / 续费跟进记录 */
    @PostMapping("/{id}/field-record")
    public Result<WorkOrderFieldRecord> addFieldRecord(@PathVariable("id") Long workOrderId,
                                                       @RequestBody WorkOrderFieldRecord req) {
        service.assertNotWaitingFixById(workOrderId);
        WorkOrderFieldRecord rec = new WorkOrderFieldRecord();
        rec.setTenantId(getTenantId());
        rec.setWorkOrderId(workOrderId);
        rec.setFollowType(req.getFollowType() != null ? req.getFollowType() : "OTHER");
        rec.setCallRecordId(req.getCallRecordId());
        rec.setContent(req.getContent());
        rec.setPhotoUrls(req.getPhotoUrls());
        var loginUser = getLoginUser();
        if (loginUser != null) {
            rec.setCreateBy(loginUser.getUsername());
            rec.setCreateByName(loginUser.getRealName());
        }
        rec.setCreateTime(new java.util.Date());
        fieldRecordManager.save(rec);
        return Result.ok(rec);
    }

    /* ==================== 附件 ==================== */

    /** 附件列表（按 workOrderId） */
    @GetMapping("/{id}/attachments")
    public Result<List<WorkOrderAttachment>> listAttachments(@PathVariable("id") Long workOrderId) {
        return Result.ok(
                attachmentManager.lambdaQuery()
                        .eq(WorkOrderAttachment::getWorkOrderId, workOrderId)
                        .orderByAsc(WorkOrderAttachment::getId)
                        .list()
        );
    }

    /**
     * 上传附件（通用）
     * @param workOrderId 任务单 ID
     * @param stepId 可空，归属节点
     * @param fileType OLD_ACCOUNT / FIELD_PHOTO / FIELD_AUDIO / CONTRACT_SCAN / OTHER_RESULT / OTHER
     */
    @PostMapping("/{id}/attachment")
    public Result<WorkOrderAttachment> uploadAttachment(
            @PathVariable("id") Long workOrderId,
            @RequestParam(value = "stepId", required = false) Long stepId,
            @RequestParam(value = "fileType", defaultValue = "OTHER") String fileType,
            @RequestParam("file") MultipartFile file) throws Exception {
        // WAITING_FIX 状态禁止上传附件
        service.assertNotWaitingFixById(workOrderId);
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
        String suffix = FileNameUtil.getSuffix(file.getOriginalFilename());
        String fileName = "WO_" + workOrderId + "_" + System.currentTimeMillis()
                + (suffix != null ? "." + suffix : "");
        File dir = new File(FileConstants.FILE_URL_WORK_ORDER);
        if (!dir.exists()) dir.mkdirs();
        file.transferTo(new File(FileConstants.FILE_URL_WORK_ORDER + File.separator + fileName));

        WorkOrderAttachment att = new WorkOrderAttachment();
        att.setTenantId(getTenantId());
        att.setWorkOrderId(workOrderId);
        att.setStepId(stepId);
        att.setFileType(fileType);
        att.setFileUrl(FileConstants.FILE_GET_WORK_ORDER + fileName);
        att.setFileName(file.getOriginalFilename());
        att.setFileSize(file.getSize());
        att.setMimeType(file.getContentType());
        att.setUploadUser(getLoginUser().getUsername());
        att.setUploadUserName(getLoginUser().getRealName());
        att.setUploadTime(new Date());
        attachmentManager.save(att);
        return Result.ok(att);
    }

    /** 删除附件（逻辑删除） */
    @DeleteMapping("/attachment/{attachmentId}")
    public Result<Void> deleteAttachment(@PathVariable Long attachmentId) {
        attachmentManager.removeById(attachmentId);
        return Result.ok();
    }
}
