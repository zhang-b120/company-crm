package com.qykj.finmaster.customer.lead.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.utils.poi.ExcelUtil;
import com.qykj.finmaster.customer.lead.entity.req.AddLeadPhoneReq;
import com.qykj.finmaster.customer.lead.entity.req.AddLeadReq;
import com.qykj.finmaster.customer.lead.entity.req.AssignLeadReq;
import com.qykj.finmaster.customer.lead.entity.req.GetLeadPageReq;
import com.qykj.finmaster.customer.lead.entity.req.UpdateLeadReq;
import com.qykj.finmaster.customer.lead.entity.resp.GetLeadExportResp;
import com.qykj.finmaster.customer.lead.service.LeadMergeCoreService;
import com.qykj.finmaster.customer.lead.service.LeadServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 线索管理Controller
 */
@RestController
@RequestMapping("/customer/lead")
public class LeadController extends BaseController {
    @Autowired
    private LeadServiceImpl leadService;

    @Autowired
    private LeadMergeCoreService leadMergeCoreService;

    /**
     * 查询线索列表
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetLeadPageReq req) {
        Long tenantId = getTenantId();
        return getDataTable(leadService.selectLeadList(req,tenantId));
    }

    /**
     * 获取线索详细信息。
     * includeDeleted=true 时一并返回已软删的线索（客户详情展示其历史关联线索时需要）。
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id,
                              @RequestParam(value = "includeDeleted", required = false, defaultValue = "false")
                              Boolean includeDeleted) {
        Long tenantId = getTenantId();
        return AjaxResult.success(leadService.selectLeadById(id, tenantId, Boolean.TRUE.equals(includeDeleted)));
    }

    /**
     * 线索查重：录入新线索时前端实时调用（手机/微信/公司名任一命中即返回已存在的线索摘要）
     * 仅返回最多 5 条，用于 UI 提示"已存在同号线索，负责人：张三"
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:list')")
    @GetMapping("/duplicate-check")
    public AjaxResult duplicateCheck(@RequestParam(required = false) String phone,
                                     @RequestParam(required = false) String wechat,
                                     @RequestParam(required = false) String companyName) {
        Long tenantId = getTenantId();
        return AjaxResult.success(leadService.duplicateCheck(phone, wechat, companyName, tenantId));
    }

    /**
     * 转化漏斗 + 转化时长 报表（Sprint 4 M1）
     * 返回各状态线索数 + 平均转化天数
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:list')")
    @GetMapping("/statistics/funnel")
    public AjaxResult statsFunnel(@RequestParam(required = false) String beginDate,
                                  @RequestParam(required = false) String endDate,
                                  @RequestParam(required = false) String source,
                                  @RequestParam(required = false) String ownerUser) {
        Long tenantId = getTenantId();
        return AjaxResult.success(leadService.getLeadFunnel(beginDate, endDate, source, ownerUser, tenantId));
    }

    /**
     * 单条线索的成交概览（Sprint 4 M2）
     * 返回：转化的客户的所有订单汇总（订单数、总金额、首单/末单时间）
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:query')")
    @GetMapping("/{id}/orders-summary")
    public AjaxResult ordersSummary(@org.springframework.web.bind.annotation.PathVariable("id") Long id) {
        Long tenantId = getTenantId();
        return AjaxResult.success(leadService.getLeadOrdersSummary(id, tenantId));
    }

    /**
     * 批量导入线索（Excel）
     * 表头顺序：线索名称 | 联系人 | 联系电话 | 微信号 | 电子邮箱 | 客户名称 | 线索来源 | 备注
     * 返回 { total, successCount, failList: [{row, leadName, reason}] }
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:add')")
    @PostMapping("/import")
    public AjaxResult importLead(@RequestParam("file") MultipartFile file) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return AjaxResult.success(leadService.importFromExcel(file, tenantId, loginUser));
    }

    /**
     * 下载线索批量导入模板（.xlsx）
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:add')")
    @GetMapping("/import-template")
    public void downloadImportTemplate(HttpServletResponse response) {
        leadService.downloadImportTemplate(response);
    }

    /**
     * 新增线索
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddLeadReq req) {
        return toAjax(leadService.insertLead(req, getTenantId(), getLoginUser()));
    }

    /**
     * 修改线索
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody UpdateLeadReq lead) {
        return toAjax(leadService.updateLead(lead, getTenantId(), getLoginUser()));
    }

    /**
     * 删除线索
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:remove')")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String ids) {
        return toAjax(leadService.deleteLeadByIds(ids));
    }

    /**
     * 批量分配线索
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:assign')")
    @PutMapping("/assign")
    public AjaxResult assign(@RequestBody AssignLeadReq req) {
        return toAjax(leadService.assignLead(req, getLoginUser()));
    }

    /**
     * 转化线索
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:convert')")
    @PutMapping("/convert/{id}")
    public AjaxResult convert(@PathVariable("id") Long id, @RequestParam String companyName, @RequestParam Integer customerType) {
        Long tenantId = getTenantId();
        return toAjax(leadService.convertToCustomer(id, companyName, customerType, tenantId, getLoginUser()));
    }

    /**
     * 导出线索列表
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:export')")
    @GetMapping("/export")
    public void export(GetLeadPageReq req, HttpServletResponse response) {
        Long tenantId = getTenantId();
        List<GetLeadExportResp> getLeadExportRespList = leadService.selectLeadListForExport(req, tenantId);
        ExcelUtil<GetLeadExportResp> util = new ExcelUtil<>(GetLeadExportResp.class);
        util.exportExcel(response, getLeadExportRespList, "线索数据");
    }

    /**
     * 合并线索（旧入口）：销售在 PC 端撞库提示时使用，按 mergeResult 覆盖 target 字段，
     * 同时走新合并核心（phone/openId 落子表 + 客户 / CC 归并）。
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:merge')")
    @PutMapping("/merge")
    public AjaxResult merge(@RequestBody Map<String, Object> params) {
        return toAjax(leadService.mergeLead(params, getTenantId()));
    }

    /**
     * 加手机号 / openId / 微信号到线索：
     *   - 命中其它源线索时触发合并（满足归属规则）
     *   - 不命中则仅在子表新增一行
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:merge')")
    @PostMapping("/{leadId}/add-phone")
    public AjaxResult addPhone(@PathVariable("leadId") Long leadId,
                                @RequestBody AddLeadPhoneReq req) {
        if (req == null) req = new AddLeadPhoneReq();
        req.setLeadId(leadId);
        return toAjax(leadMergeCoreService.addPhone(req));
    }

    /**
     * 查询线索的所有通道（手机号 / openId / 微信号）。
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:query')")
    @GetMapping("/{leadId}/phones")
    public AjaxResult listPhones(@PathVariable("leadId") Long leadId) {
        return AjaxResult.success(leadMergeCoreService.listPhones(leadId));
    }

    /**
     * 把指定通道设为主通道（同步 sale_lead 主表的 phone/openId/wechat）。
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:merge')")
    @PutMapping("/{leadId}/phones/{phoneId}/primary")
    public AjaxResult setPrimaryPhone(@PathVariable("leadId") Long leadId,
                                       @PathVariable("phoneId") Long phoneId) {
        return toAjax(leadMergeCoreService.setPrimaryPhone(leadId, phoneId));
    }

    /**
     * 编辑通道行（弹窗内编辑）。
     * 主通道行编辑后自动同步 sale_lead 主表的 phone/openId/wechat。
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:merge')")
    @PutMapping("/{leadId}/phones/{phoneId}")
    public AjaxResult updatePhone(@PathVariable("leadId") Long leadId,
                                   @PathVariable("phoneId") Long phoneId,
                                   @RequestBody AddLeadPhoneReq req) {
        if (req == null) req = new AddLeadPhoneReq();
        return toAjax(leadMergeCoreService.updatePhone(leadId, phoneId, req));
    }
}
