package com.qykj.finmaster.registerAddress.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.registerAddress.domain.RegisterAddress;
import com.qykj.finmaster.registerAddress.domain.req.AddRegisterAddressReq;
import com.qykj.finmaster.registerAddress.domain.req.GetRegisterAddressListReq;
import com.qykj.finmaster.registerAddress.domain.req.UpdateRegisterAddressReq;
import com.qykj.finmaster.registerAddress.service.RegisterAddressServiceImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 注册地址Controller
 */
@RestController
@RequestMapping("/registerAddress")
public class RegisterAddressController extends BaseController {

    @Autowired
    private RegisterAddressServiceImpl registerAddressService;

    /**
     * 查询注册地址列表
     */
    @GetMapping("/list")
    public TableDataInfo list(GetRegisterAddressListReq req) {
        Long tenantId = getTenantId();
        List<com.qykj.finmaster.registerAddress.domain.resp.GetRegisterAddressListResp> list = registerAddressService.getRegisterAddressList(req, tenantId);
        return getDataTable(list);
    }

    /**
     * 获取注册地址详情
     */
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        RegisterAddress resp = registerAddressService.getRegisterAddressDetail(id);
        return AjaxResult.success(resp);
    }

    /**
     * 新增注册地址
     */
    @PostMapping("/add")
    public AjaxResult add(@RequestBody AddRegisterAddressReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(registerAddressService.addRegisterAddress(req, tenantId, loginUser));
    }

    /**
     * 编辑注册地址
     */
    @PostMapping("/update")
    public AjaxResult update(@RequestBody UpdateRegisterAddressReq req) {
        Long tenantId = getTenantId();
        LoginUser loginUser = getLoginUser();
        return toAjax(registerAddressService.updateRegisterAddress(req, tenantId, loginUser));
    }

    /**
     * 删除注册地址
     */
    @PostMapping("/delete")
    public AjaxResult delete(@RequestParam Long id) {
        return toAjax(registerAddressService.deleteRegisterAddress(id, getLoginUser()));
    }

    /**
     * 更新地址状态
     */
    @PostMapping("/updateStatus")
    public AjaxResult updateStatus(@RequestParam Long id, @RequestParam Integer status) {
        return toAjax(registerAddressService.updateAddressStatus(id, status, getLoginUser()));
    }

    /**
     * 启用/禁用地址
     */
    @PostMapping("/updateEnable")
    public AjaxResult updateEnable(@RequestParam Long id, @RequestParam Integer enable) {
        return toAjax(registerAddressService.updateAddressEnable(id, enable, getLoginUser()));
    }
}
