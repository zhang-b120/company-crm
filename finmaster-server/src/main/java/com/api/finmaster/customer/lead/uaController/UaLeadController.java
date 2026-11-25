package com.qykj.finmaster.customer.lead.uaController;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.customer.lead.entity.req.UaAddLeadReq;
import com.qykj.finmaster.customer.lead.entity.req.AddRecommendCodeReq;
import com.qykj.finmaster.customer.lead.service.LeadServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author zhb
 * @Date 2025-05-29
 * @Description
 */
@RestController
@RequestMapping("/ua/customer/lead")
public class UaLeadController {

    @Autowired
    private LeadServiceImpl leadService;


    /**
     * 新增线索
     */
    @PermitAll
    @PostMapping("/uaAdd")
    public AjaxResult uaAdd(@RequestBody UaAddLeadReq req) {
        return AjaxResult.success(leadService.uaAdd(req));
    }


    /**
     * 设置推荐码
     */
    @PermitAll
    @PostMapping("/uaAddRecommendCode")
    public AjaxResult uaAddRecommendCode(@RequestBody AddRecommendCodeReq req) {
        return AjaxResult.success(leadService.uaAddRecommendCode(req));
    }
}