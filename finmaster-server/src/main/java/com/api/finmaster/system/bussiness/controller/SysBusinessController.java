package com.qykj.finmaster.system.bussiness.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.bussiness.domain.req.AddBusinessProductReq;
import com.qykj.finmaster.system.bussiness.domain.req.AddBusinessReq;
import com.qykj.finmaster.system.bussiness.domain.req.GetBusinessListReq;
import com.qykj.finmaster.system.bussiness.domain.req.UpdateBusinessReq;
import com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessDetailResp;
import com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessListResp;
import com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp;
import com.qykj.finmaster.system.bussiness.service.SysBusinessServiceImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 业务管理Controller
 */
@RestController
@RequestMapping("/system/business")
public class SysBusinessController extends BaseController {

    @Autowired
    private SysBusinessServiceImpl sysBusinessService;

    /**
     * 查询业务列表
     */
    @GetMapping("/list")
    public TableDataInfo list(GetBusinessListReq req) {
        Long tenantId = getTenantId();
        List<GetBusinessListResp> list = sysBusinessService.getBusinessList(req, tenantId);
        return getDataTable(list);
    }

    /**
     * 获取业务详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        GetBusinessDetailResp resp = sysBusinessService.getBusinessDetail(id);
        return AjaxResult.success(resp);
    }

    /**
     * 新增业务
     */
    @PostMapping("/add")
    public AjaxResult add(@RequestBody AddBusinessReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(sysBusinessService.addBusiness(req, tenantId, loginUser));
    }

    /**
     * 编辑业务
     */
    @PostMapping("/update")
    public AjaxResult update(@RequestBody UpdateBusinessReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(sysBusinessService.updateBusiness(req, tenantId, loginUser));
    }

    /**
     * 删除业务
     */
    @PostMapping("/delete")
    public AjaxResult delete(@RequestParam Long id) {
        return toAjax(sysBusinessService.deleteBusiness(id, getLoginUser()));
    }

    /**
     * 查询业务下关联产品列表
     */
    @GetMapping("/{id}/products")
    public AjaxResult getBusinessProducts(@PathVariable("id") Long id) {
        List<GetBusinessProductResp> list = sysBusinessService.getBusinessProductList(id);
        return AjaxResult.success(list);
    }

    /**
     * 保存业务产品关联（覆盖式）
     */
    @PostMapping("/product/add")
    public AjaxResult addBusinessProduct(@RequestBody AddBusinessProductReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        if (ObjectUtils.isEmpty(req.getBusinessId())) {
            return AjaxResult.error("未获取到业务ID！");
        }
        return toAjax(sysBusinessService.addBusinessProduct(req, tenantId, loginUser));
    }
}

