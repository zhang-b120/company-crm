package com.qykj.finmaster.order.service;

import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.manager.impl.ContractManagerImpl;
import com.qykj.finmaster.customer.company.domain.CompanyService;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CompanyServiceManagerImpl;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.order.domain.ServiceTransferItem;
import com.qykj.finmaster.order.domain.req.AddServiceTransferOrderReq;
import com.qykj.finmaster.order.domain.resp.ServiceTransferContextResp;
import com.qykj.finmaster.order.manager.ServiceTransferItemManager;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.bussiness.domain.SysBusiness;
import com.qykj.finmaster.system.bussiness.manager.impl.SysBusinessManagerImpl;
import com.qykj.finmaster.workorder.constants.WorkOrderSourceType;
import com.qykj.finmaster.workorder.constants.WorkOrderTaskType;
import com.qykj.finmaster.workorder.domain.WorkOrder;
import com.qykj.finmaster.workorder.domain.req.CreateWorkOrderReq;
import com.qykj.finmaster.workorder.manager.WorkOrderManager;
import com.qykj.finmaster.workorder.service.WorkOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 服务转移（合同驱动版本）。
 * <p>
 * 入口：合同列表页 →「服务转移」按钮 → 弹窗 → 提交。
 * 1 工单 = 1 条转移对（K=1）；不创建任何 Order / Contract，只落 service_transfer_item + WO。
 * <p>
 * 流程：
 * <ol>
 *   <li>校验入参（源合同 / 源公司服务 / 目标公司服务 / 目标新到期日 / 同客户 / 业务一致 / 重复转移）</li>
 *   <li>落库 service_transfer_item（PENDING，work_order_id 暂空）</li>
 *   <li>派发 SERVICE_TRANSFER 工单（3 节点：REFUND_REVIEW → REFUND_CANCEL_TAX → REFUND_TRANSFER）</li>
 *   <li>回填 service_transfer_item.work_order_id</li>
 * </ol>
 * Node 3 完成时，WorkOrderService 钩子负责：源服务 CANCELLED + 相关合同 TERMINATE + 目标服务 actualEndDate 顺延。
 *
 * @Author zhb
 * @Date 2026-05-12
 */
@Slf4j
@Service
public class ServiceTransferService {

    @Autowired
    private ServiceTransferItemManager serviceTransferItemManager;

    @Autowired
    private CompanyServiceManagerImpl companyServiceManager;

    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;

    @Autowired
    private ContractManagerImpl contractManager;

    @Autowired
    private SysBusinessManagerImpl sysBusinessManager;

    @Autowired
    private WorkOrderManager workOrderManager;

    @Lazy
    @Autowired
    private WorkOrderService workOrderService;

    @Transactional(rollbackFor = Exception.class)
    public Boolean createServiceTransfer(AddServiceTransferOrderReq req, Long tenantId, LoginUser loginUser) {
        // ============ 1. 基础必填校验 ============
        if (req.getSourceContractId() == null)        throw new BusinessException("缺少源合同 ID");
        if (req.getSourceCompanyId() == null)         throw new BusinessException("缺少源公司 ID");
        if (req.getSourceBusinessId() == null)        throw new BusinessException("缺少源业务 ID");
        if (req.getSourceCompanyServiceId() == null)  throw new BusinessException("请选择源公司服务");
        if (req.getTargetCompanyId() == null)         throw new BusinessException("请选择目标公司");
        if (req.getTargetCompanyServiceId() == null)  throw new BusinessException("请选择目标公司服务");
        if (req.getTargetActualEndDate() == null)     throw new BusinessException("请填写目标公司服务的新到期日");
        if (StringUtils.isBlank(req.getRefundReason())) throw new BusinessException("请填写转移原因");

        // ============ 2. 源合同校验 ============
        Contract sourceContract = contractManager.getById(req.getSourceContractId());
        if (sourceContract == null) throw new BusinessException("源合同不存在：id=" + req.getSourceContractId());
        if (!Objects.equals(sourceContract.getCompanyId(), req.getSourceCompanyId())) {
            throw new BusinessException("源合同与源公司不匹配");
        }
        if (!Objects.equals(sourceContract.getBusinessId(), req.getSourceBusinessId())) {
            throw new BusinessException("源合同与源业务不匹配");
        }

        // 客户：优先取入参，其次取源公司
        Long customerId = req.getCustomerId();
        if (customerId == null) {
            CustomerCompany srcCompany = customerCompanyManager.getById(req.getSourceCompanyId());
            if (srcCompany == null) throw new BusinessException("源公司不存在：id=" + req.getSourceCompanyId());
            customerId = srcCompany.getCustomerId();
        }
        if (customerId == null) throw new BusinessException("无法确定客户，请联系管理员");

        // ============ 3. 源服务校验 ============
        CompanyService srcSvc = companyServiceManager.getById(req.getSourceCompanyServiceId());
        if (srcSvc == null) throw new BusinessException("源公司服务不存在：id=" + req.getSourceCompanyServiceId());
        if (!Objects.equals(srcSvc.getCompanyId(), req.getSourceCompanyId())) {
            throw new BusinessException("源服务不属于源公司");
        }
        if (!Objects.equals(srcSvc.getBusinessId(), req.getSourceBusinessId())) {
            throw new BusinessException("源服务的业务与源合同不一致");
        }

        // ============ 4. 目标服务校验 ============
        CompanyService tgtSvc = companyServiceManager.getById(req.getTargetCompanyServiceId());
        if (tgtSvc == null) throw new BusinessException("目标公司服务不存在：id=" + req.getTargetCompanyServiceId());
        if (!Objects.equals(tgtSvc.getCompanyId(), req.getTargetCompanyId())) {
            throw new BusinessException("目标服务不属于目标公司");
        }
        // 业务必须相同（不允许跨业务转移）
        if (!Objects.equals(tgtSvc.getBusinessId(), req.getSourceBusinessId())) {
            throw new BusinessException("目标服务的业务必须与源相同（不允许跨业务转移）");
        }
        // 源 != 目标
        if (Objects.equals(req.getSourceCompanyServiceId(), req.getTargetCompanyServiceId())) {
            throw new BusinessException("目标服务不能与源服务相同");
        }
        if (Objects.equals(req.getSourceCompanyId(), req.getTargetCompanyId())) {
            throw new BusinessException("目标公司不能与源公司相同");
        }

        // ============ 5. 目标公司同客户校验 ============
        CustomerCompany tgtCompany = customerCompanyManager.getById(req.getTargetCompanyId());
        if (tgtCompany == null) throw new BusinessException("目标公司不存在：id=" + req.getTargetCompanyId());
        if (!Objects.equals(tgtCompany.getCustomerId(), customerId)) {
            throw new BusinessException("目标公司不属于同一客户，禁止跨客户转移");
        }

        // ============ 6. 重复发起拦截（PENDING / DONE 均算占用，FAILED 允许重试） ============
        // 6a. 同源服务（同 (公司, 业务) 粒度）
        long existingBySvc = serviceTransferItemManager.lambdaQuery()
                .eq(ServiceTransferItem::getSourceCompanyServiceId, req.getSourceCompanyServiceId())
                .ne(ServiceTransferItem::getProcessStatus, "FAILED")
                .count();
        if (existingBySvc > 0) {
            throw new BusinessException("该源服务已发起过服务转移，禁止重复");
        }
        // 6b. 同源合同（一份合同只能发起一次转移；驳回后会物理删除 transfer_item，所以不会误拦）
        long existingByCt = serviceTransferItemManager.lambdaQuery()
                .eq(ServiceTransferItem::getSourceContractId, req.getSourceContractId())
                .ne(ServiceTransferItem::getProcessStatus, "FAILED")
                .count();
        if (existingByCt > 0) {
            throw new BusinessException("该源合同已发起过服务转移，禁止重复");
        }

        // ============ 7. 落 service_transfer_item（work_order_id 暂空） ============
        Date now = new Date();
        ServiceTransferItem sti = new ServiceTransferItem();
        sti.setTenantId(tenantId);
        sti.setSourceContractId(req.getSourceContractId());
        sti.setSourceContractDetailId(req.getSourceContractDetailId());
        sti.setSourceCompanyId(req.getSourceCompanyId());
        sti.setSourceBusinessId(req.getSourceBusinessId());
        sti.setSourceCompanyServiceId(req.getSourceCompanyServiceId());
        sti.setTargetCompanyId(req.getTargetCompanyId());
        sti.setTargetCompanyServiceId(req.getTargetCompanyServiceId());
        sti.setTransferMonths(req.getTransferMonths());
        sti.setTargetActualEndDate(req.getTargetActualEndDate());
        sti.setProcessStatus("PENDING");
        sti.setCreateTime(now);
        if (loginUser != null) {
            sti.setCreateBy(loginUser.getUsername());
            sti.setCreateByName(loginUser.getRealName());
        }
        serviceTransferItemManager.save(sti);

        // ============ 8. 派发 SERVICE_TRANSFER 工单 ============
        try {
            CreateWorkOrderReq cr = new CreateWorkOrderReq();
            cr.setTaskType(WorkOrderTaskType.SERVICE_TRANSFER.name());
            cr.setSourceType(WorkOrderSourceType.MANUAL.name());
            cr.setSourceId(sti.getId());                       // sourceId = service_transfer_item.id
            cr.setSourceNumber(sourceContract.getContractNo()); // sourceNumber = 源合同号
            cr.setCustomerId(customerId);
            cr.setCompanyId(req.getSourceCompanyId());
            cr.setTitle("服务转移 - " + StringUtils.defaultString(req.getCustomerName(),
                    "客户 #" + customerId));
            String desc = "服务转移 · 来源合同 " + sourceContract.getContractNo()
                    + "\n源服务 → 目标服务 id=" + req.getSourceCompanyServiceId()
                    + " → " + req.getTargetCompanyServiceId()
                    + "\n目标新到期日：" + req.getTargetActualEndDate()
                    + "\n转移原因：" + req.getRefundReason();
            cr.setDescription(desc);
            cr.setRemark(req.getRemark());

            Map<String, Object> meta = new HashMap<>();
            meta.put("refundReason", req.getRefundReason());
            meta.put("refundMethod", "SERVICE_TRANSFER");
            meta.put("sourceContractNo", sourceContract.getContractNo());
            meta.put("sourceContractId", sourceContract.getId());
            meta.put("targetActualEndDate", req.getTargetActualEndDate().toString());
            cr.setMetaJson(com.alibaba.fastjson.JSON.toJSONString(meta));

            String reviewer = StringUtils.isNotBlank(req.getReviewerUser())
                    ? req.getReviewerUser()
                    : (loginUser != null ? loginUser.getUsername() : null);
            cr.setAssignedOwnerUser(reviewer);

            WorkOrder wo = workOrderService.createWorkOrder(cr, loginUser, tenantId);
            if (wo != null && wo.getId() != null) {
                serviceTransferItemManager.lambdaUpdate()
                        .eq(ServiceTransferItem::getId, sti.getId())
                        .set(ServiceTransferItem::getWorkOrderId, wo.getId())
                        .update();
            }
        } catch (Exception e) {
            log.error("[服务转移] 自动派发 SERVICE_TRANSFER 工单失败，请在工单页手动新建", e);
            throw new BusinessException("派发服务转移工单失败：" + e.getMessage());
        }
        return Boolean.TRUE;
    }

    /**
     * 按工单 id 拉服务转移上下文（供工单详情页渲染完整信息块）。
     * 聚合源/目标公司、源合同、源/目标公司服务、业务名、处理状态等富字段。
     */
    public ServiceTransferContextResp getContextByWorkOrderId(Long workOrderId) {
        if (workOrderId == null) return null;
        ServiceTransferItem item = serviceTransferItemManager.lambdaQuery()
                .eq(ServiceTransferItem::getWorkOrderId, workOrderId)
                .last("LIMIT 1")
                .one();
        if (item == null) return null;

        ServiceTransferContextResp resp = new ServiceTransferContextResp();
        resp.setItemId(item.getId());
        resp.setWorkOrderId(item.getWorkOrderId());
        resp.setTransferMonths(item.getTransferMonths());
        resp.setTargetActualEndDate(item.getTargetActualEndDate());
        resp.setProcessStatus(item.getProcessStatus());
        resp.setProcessRemark(item.getProcessRemark());

        // 从工单 meta 取转移原因
        try {
            WorkOrder wo = workOrderManager.getById(workOrderId);
            if (wo != null && com.qykj.finmaster.common.utils.StringUtils.isNotBlank(wo.getMetaJson())) {
                com.alibaba.fastjson.JSONObject meta = com.alibaba.fastjson.JSON.parseObject(wo.getMetaJson());
                resp.setRefundReason(meta != null ? meta.getString("refundReason") : null);
            }
        } catch (Exception ignore) { /* 取不到原因不阻断 */ }

        // 源公司
        CustomerCompany srcCompany = customerCompanyManager.getById(item.getSourceCompanyId());
        if (srcCompany != null) {
            ServiceTransferContextResp.CompanyBrief b = new ServiceTransferContextResp.CompanyBrief();
            b.setId(srcCompany.getId());
            b.setCompanyName(srcCompany.getCompanyName());
            b.setTaxNumber(srcCompany.getTaxNumber());
            b.setLegalName(srcCompany.getLegalName());
            b.setLegalPhone(srcCompany.getLegalPhone());
            b.setCompanyType(srcCompany.getCompanyType());
            resp.setSourceCompany(b);
        }

        // 目标公司
        CustomerCompany tgtCompany = customerCompanyManager.getById(item.getTargetCompanyId());
        if (tgtCompany != null) {
            ServiceTransferContextResp.CompanyBrief b = new ServiceTransferContextResp.CompanyBrief();
            b.setId(tgtCompany.getId());
            b.setCompanyName(tgtCompany.getCompanyName());
            b.setTaxNumber(tgtCompany.getTaxNumber());
            b.setLegalName(tgtCompany.getLegalName());
            b.setLegalPhone(tgtCompany.getLegalPhone());
            b.setCompanyType(tgtCompany.getCompanyType());
            resp.setTargetCompany(b);
        }

        // 源合同
        Contract srcContract = item.getSourceContractId() != null
                ? contractManager.getById(item.getSourceContractId())
                : null;
        if (srcContract != null) {
            ServiceTransferContextResp.ContractBrief b = new ServiceTransferContextResp.ContractBrief();
            b.setId(srcContract.getId());
            b.setContractNo(srcContract.getContractNo());
            b.setContractType(srcContract.getContractType());
            b.setStatus(srcContract.getStatus());
            b.setContractStartTime(srcContract.getContractStartTime());
            b.setContractEndTime(srcContract.getContractEndTime());
            b.setContractPrice(srcContract.getContractPrice());
            resp.setSourceContract(b);
        }

        // 源服务
        CompanyService srcSvc = companyServiceManager.getById(item.getSourceCompanyServiceId());
        if (srcSvc != null) {
            ServiceTransferContextResp.ServiceBrief b = new ServiceTransferContextResp.ServiceBrief();
            b.setCompanyServiceId(srcSvc.getId());
            b.setEffectiveDate(srcSvc.getEffectiveDate());
            b.setEndDate(srcSvc.getEndDate());
            b.setActualEndDate(srcSvc.getActualEndDate());
            b.setStatus(srcSvc.getStatus());
            b.setServiceName(resolveServiceName(srcSvc.getCompanyId(), srcSvc.getId()));
            resp.setSourceService(b);
        }

        // 目标服务
        CompanyService tgtSvc = companyServiceManager.getById(item.getTargetCompanyServiceId());
        if (tgtSvc != null) {
            ServiceTransferContextResp.ServiceBrief b = new ServiceTransferContextResp.ServiceBrief();
            b.setCompanyServiceId(tgtSvc.getId());
            b.setEffectiveDate(tgtSvc.getEffectiveDate());
            b.setEndDate(tgtSvc.getEndDate());
            b.setActualEndDate(tgtSvc.getActualEndDate());
            b.setStatus(tgtSvc.getStatus());
            b.setServiceName(resolveServiceName(tgtSvc.getCompanyId(), tgtSvc.getId()));
            resp.setTargetService(b);
        }

        // 业务名
        if (item.getSourceBusinessId() != null) {
            SysBusiness biz = sysBusinessManager.getById(item.getSourceBusinessId());
            if (biz != null) resp.setBusinessName(biz.getBusinessName());
        }
        return resp;
    }

    /** 从 listAllServicesByCompanyIdForRenewal 反查 serviceName（CompanyService 实体没有直接的 serviceName 字段）。 */
    private String resolveServiceName(Long companyId, Long companyServiceId) {
        if (companyId == null || companyServiceId == null) return null;
        try {
            return companyServiceManager.getBaseMapper()
                    .listAllServicesByCompanyIdForRenewal(companyId).stream()
                    .filter(r -> java.util.Objects.equals(r.getCompanyServiceId(), companyServiceId))
                    .findFirst()
                    .map(r -> r.getServiceName())
                    .orElse(null);
        } catch (Exception e) {
            log.warn("[服务转移] resolveServiceName 失败 companyId={}, companyServiceId={}",
                    companyId, companyServiceId, e);
            return null;
        }
    }
}
