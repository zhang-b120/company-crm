package com.qykj.finmaster.oa.trip.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.oa.trip.domain.OaBusinessTrip;
import com.qykj.finmaster.oa.trip.domain.req.AddBusinessTripReq;
import com.qykj.finmaster.oa.trip.domain.req.GetBusinessTripListReq;
import com.qykj.finmaster.oa.trip.service.BusinessTripServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 出差管理Controller
 */
@RestController
@RequestMapping("/oa/trip")
public class BusinessTripController {

    @Autowired
    private BusinessTripServiceImpl businessTripService;

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
     * 查询出差列表
     */
    @PreAuthorize("@ss.hasPermi('oa:trip:list')")
    @GetMapping("/list")
    public TableDataInfo list(GetBusinessTripListReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        PageHelper.startPage(req.getPageNum(), req.getPageSize());
        List<OaBusinessTrip> list = businessTripService.getList(req, tenantId, loginUser);
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(200);
        rspData.setRows(list);
        rspData.setMsg("查询成功");
        rspData.setTotal(new PageInfo<>(list).getTotal());
        return rspData;
    }

    /**
     * 获取出差详情（含行程段）
     */
    @PreAuthorize("@ss.hasPermi('oa:trip:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        Map<String, Object> detail = businessTripService.getDetail(id);
        return AjaxResult.success(detail);
    }

    /**
     * 新增出差申请
     */
    @PreAuthorize("@ss.hasPermi('oa:trip:add')")
    @PostMapping
    public AjaxResult add(@RequestBody AddBusinessTripReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long id = businessTripService.addTrip(req, tenantId, loginUser);
        return AjaxResult.success(id);
    }

    /**
     * 修改出差申请
     */
    @PreAuthorize("@ss.hasPermi('oa:trip:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody AddBusinessTripReq req) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        businessTripService.updateTrip(req, tenantId, loginUser);
        return AjaxResult.success();
    }

    /**
     * 删除出差申请
     */
    @PreAuthorize("@ss.hasPermi('oa:trip:remove')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        businessTripService.deleteTrip(id);
        return AjaxResult.success();
    }

    /**
     * 提交出差申请（发起审批）
     */
    @PreAuthorize("@ss.hasPermi('oa:trip:submit')")
    @PostMapping("/submit/{id}")
    public AjaxResult submit(@PathVariable Long id) {
        LoginUser loginUser = getLoginUser();
        Long tenantId = getTenantId();
        Long instanceId = businessTripService.submitTrip(id, tenantId, loginUser);
        return AjaxResult.success(instanceId);
    }

    /**
     * 关联报销单
     */
    @PreAuthorize("@ss.hasPermi('oa:trip:edit')")
    @PostMapping("/{id}/link-expense/{expenseId}")
    public AjaxResult linkExpense(@PathVariable Long id, @PathVariable Long expenseId) {
        LoginUser loginUser = getLoginUser();
        businessTripService.linkExpense(id, expenseId, loginUser);
        return AjaxResult.success();
    }
}
