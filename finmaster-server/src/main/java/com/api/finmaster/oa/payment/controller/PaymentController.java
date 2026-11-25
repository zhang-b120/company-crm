package com.qykj.finmaster.oa.payment.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.payment.domain.OaPayment;
import com.qykj.finmaster.oa.payment.domain.req.AddPaymentReq;
import com.qykj.finmaster.oa.payment.domain.req.GetPaymentListReq;
import com.qykj.finmaster.oa.payment.service.PaymentServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 付款管理Controller
 */
@RestController("oaPaymentController")
@RequestMapping("/oa/payment")
public class PaymentController {

    @Autowired
    private PaymentServiceImpl paymentService;

    /**
     * 获取当前登录用户
     */
    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (LoginUser) auth.getPrincipal();
    }

    /**
     * 获取租户ID
     */
    private Long getTenantId() {
        LoginUser loginUser = getLoginUser();
        return loginUser.getTenantId();
    }

    /**
     * 查询付款列表
     */
    @PreAuthorize("@ss.hasPermi('oa:payment:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetPaymentListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaPayment> list = paymentService.getList(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 新增付款申请
     */
    @PreAuthorize("@ss.hasPermi('oa:payment:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddPaymentReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = paymentService.addPayment(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 提交付款申请（发起审批）
     */
    @PreAuthorize("@ss.hasPermi('oa:payment:submit')")
    @PostMapping("/submit/{id}")
    public AjaxResult submit(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long instanceId = paymentService.submitPayment(id, tenantId, loginUser);
        return AjaxResult.success(instanceId);
    }

    /**
     * 获取付款详情
     */
    @PreAuthorize("@ss.hasPermi('oa:payment:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        OaPayment payment = paymentService.getDetail(id);
        return AjaxResult.success(payment);
    }

    /**
     * 确认打款
     */
    @PreAuthorize("@ss.hasPermi('oa:payment:confirmPaid')")
    @PostMapping("/confirm-paid/{id}")
    public AjaxResult confirmPaid(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        paymentService.confirmPaid(id, loginUser);
        return AjaxResult.success();
    }

    /**
     * 批量确认打款
     */
    @PreAuthorize("@ss.hasPermi('oa:payment:confirmPaid')")
    @PostMapping("/batch-confirm-paid")
    public AjaxResult batchConfirmPaid(@RequestBody Map<String, List<Long>> body) {
        LoginUser loginUser = getLoginUser();
        List<Long> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return AjaxResult.error("请选择要打款的记录");
        }
        Map<String, Object> result = paymentService.batchConfirmPaid(ids, loginUser);
        return AjaxResult.success(result);
    }
}
