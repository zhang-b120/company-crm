package com.qykj.finmaster.customer.leadLog.controller;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.customer.leadLog.entity.LeadLog;
import com.qykj.finmaster.customer.leadLog.entity.req.AddLeadLog;
import com.qykj.finmaster.customer.leadLog.service.LeadLogServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 线索跟进日志Controller
 */
@RestController
@RequestMapping("/customer/lead/log")
public class LeadLogController extends BaseController {

    @Autowired
    private LeadLogServiceImpl leadLogService;

    /**
     * 添加跟进日志
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:follow')")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody AddLeadLog req) {
        Long tenantId = getTenantId();
        return toAjax(leadLogService.addLeadLog(req,tenantId, getLoginUser()));
    }

    /**
     * 获取线索的跟进日志列表
     */
    @PreAuthorize("@ss.hasPermi('customer:lead:list')")
    @GetMapping("/list/{leadId}")
    public AjaxResult list(@PathVariable("leadId") Long leadId) {
        Long tenantId = getTenantId();
        List<LeadLog> list = leadLogService.getLeadLogList(leadId, tenantId);
        return AjaxResult.success(list);
    }
}
