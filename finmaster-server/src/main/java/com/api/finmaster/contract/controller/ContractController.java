package com.qykj.finmaster.contract.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.contract.domain.req.AddContractReq;
import com.qykj.finmaster.contract.domain.req.GenerateContractFileReq;
import com.qykj.finmaster.contract.domain.req.GetContractListReq;
import com.qykj.finmaster.contract.domain.req.UpdateContractReq;
import com.qykj.finmaster.contract.domain.resp.GetContractDetailResp;
import com.qykj.finmaster.contract.domain.resp.OrderDetailForContractResp;
import com.qykj.finmaster.contract.service.ContractServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 合同Controller
 */
@RestController
@RequestMapping("/contract")
public class ContractController extends BaseController {

    @Autowired
    private ContractServiceImpl contractService;

    @Autowired
    private com.qykj.finmaster.order.service.ServiceTransferService serviceTransferService;

    /**
     * 服务转移（合同驱动版本）：
     * 入口在合同列表页 → 弹窗选源服务/目标公司/目标新到期日 → 提交。
     * 不创建任何 Order / 续费 Contract；Node 3 完成时由钩子直接改 company_service + TERMINATE 关联合同。
     */
    @PostMapping("/serviceTransfer")
    public AjaxResult serviceTransfer(
            @RequestBody com.qykj.finmaster.order.domain.req.AddServiceTransferOrderReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(serviceTransferService.createServiceTransfer(req, tenantId, loginUser));
    }

    /** 服务转移工单详情页：聚合源/目标公司、源合同、源/目标服务等富字段 */
    @GetMapping("/serviceTransfer/byWorkOrder/{workOrderId}")
    public AjaxResult serviceTransferContext(@PathVariable("workOrderId") Long workOrderId) {
        return AjaxResult.success(serviceTransferService.getContextByWorkOrderId(workOrderId));
    }

    /**
     * 查询合同列表
     */
    @GetMapping("/list")
    public TableDataInfo list(GetContractListReq req) {
        Long tenantId = getTenantId();
        List<com.qykj.finmaster.contract.domain.resp.GetContractListResp> list = contractService.getContractList(req, tenantId);
        return getDataTable(list);
    }

    /**
     * 获取合同详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        GetContractDetailResp resp = contractService.getContractDetail(id);
        return AjaxResult.success(resp);
    }

    /**
     * 新增合同
     */
    @PostMapping("/add")
    public AjaxResult add(@RequestBody AddContractReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(contractService.addContract(req, tenantId, loginUser));
    }

    /**
     * 编辑合同
     */
    @PostMapping("/update")
    public AjaxResult update(@RequestBody UpdateContractReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(contractService.updateContract(req, tenantId, loginUser));
    }

    /**
     * 删除合同（已废弃，保留接口但不再提供删除能力）
     */
    @Deprecated
    @PostMapping("/delete")
    public AjaxResult delete(@RequestParam Long id) {
        throw new BusinessException("合同不支持删除操作，请使用废弃功能！");
    }

    /**
     * 更新合同状态
     */
    @PostMapping("/updateStatus")
    public AjaxResult updateStatus(@RequestParam Long id, @RequestParam String status) {
        return toAjax(contractService.updateContractStatus(id, status, getLoginUser()));
    }

    /**
     * 根据公司和业务自动计算合同类型
     */
    @GetMapping("/autoType")
    public AjaxResult getAutoContractType(@RequestParam Long companyId, @RequestParam Long businessId) {
        Long tenantId = getTenantId();
        String contractType = contractService.getAutoContractType(companyId, businessId, tenantId);
        return AjaxResult.success(contractType);
    }

    /**
     * 获取指定公司+业务下可用于合同关联的订单明细。
     * 编辑合同时传 contractId，当前合同已占用的明细仍会返回，便于回显。
     */
    @GetMapping("/orderDetails")
    public AjaxResult getOrderDetails(@RequestParam Long companyId, @RequestParam Long businessId,
                                     @RequestParam(required = false) Long contractId) {
        Long tenantId = getTenantId();
        java.util.List<OrderDetailForContractResp> list = contractService.getOrderDetailsForContract(companyId, businessId, tenantId, contractId);
        return AjaxResult.success(list);
    }

    /**
     * 订单→合同 一键生成：根据订单预装合同表单初值
     * 按 (companyId + businessId) 分组，一组对应一张合同
     * 前端拿到后若只有 1 组 → 直接打开合同新增对话框并预填；多组 → 让用户选
     */
    @GetMapping("/quick-from-order/{orderId}")
    public AjaxResult quickFromOrder(@PathVariable("orderId") Long orderId) {
        Long tenantId = getTenantId();
        return AjaxResult.success(contractService.prepareQuickContractFromOrder(orderId, tenantId));
    }

    /**
     * 续费列表→续费合同 一键生成：根据 CompanyService 预装续费合同初值
     * 自动锁定 contractType=contract_renew、按上次合同复制价格/明细、周期默认顺延
     */
    @GetMapping("/quick-renew/{companyServiceId}")
    public AjaxResult quickRenew(@PathVariable("companyServiceId") Long companyServiceId) {
        Long tenantId = getTenantId();
        return AjaxResult.success(contractService.prepareQuickRenewContract(companyServiceId, tenantId));
    }

    /**
     * 生成合同文件 - 第一步：返回 16 个占位符的预填值供前端预览/编辑。不写文件。
     * 入参 contractId（必填） + supplement（选填，作为 ${supplement} 的初始值）。
     */
    @PostMapping("/generateFile/preparePreview")
    public AjaxResult prepareGenerateFilePreview(@RequestBody GenerateContractFileReq req) {
        Long tenantId = getTenantId();
        java.util.Map<String, String> values = contractService.prepareContractFileValues(
                req.getContractId(), req.getSupplement(), tenantId);
        return AjaxResult.success(values);
    }

    /**
     * 生成合同文件 - 预览渲染：用 values 在内存里渲染模板并转成 HTML 返回，不落盘。
     * 前端"刷新预览"按钮调用。
     */
    @PostMapping("/generateFile/previewHtml")
    public AjaxResult previewHtml(@RequestBody GenerateContractFileReq req) {
        Long tenantId = getTenantId();
        String html = contractService.renderContractFilePreviewHtml(
                req.getContractId(), req.getValues(), tenantId);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("html", html);
        return AjaxResult.success(data);
    }

    /**
     * 生成合同文件 - 第二步：使用预览页编辑后的 values 实际渲染并落盘 docx，返回文件 URL。
     * 已生成的合同会拒绝重复生成。
     */
    @PostMapping("/generateFile")
    public AjaxResult generateFile(@RequestBody GenerateContractFileReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        String url = contractService.generateContractFileWithValues(
                req.getContractId(), req.getValues(), loginUser, tenantId);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("contractFileUrl", url);
        return AjaxResult.success(data);
    }
}
