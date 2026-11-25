package com.qykj.finmaster.system.serviceMaintenance.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.system.serviceMaintenance.domain.req.*;
import com.qykj.finmaster.system.serviceMaintenance.service.ServiceMaintenanceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/serviceMaintenance")
public class ServiceMaintenanceController extends BaseController {
    @Autowired
    private ServiceMaintenanceServiceImpl serviceMaintenanceService;

    /**
     * 获取服务维护列表
     *
     * @param req
     * @return
     */
    @GetMapping("/list")
    public TableDataInfo getServiceMaintenanceList(GetServiceMaintenanceListReq req) {
        return getDataTable(serviceMaintenanceService.getServiceMaintenanceList(req));
    }

    /**
     * 获取服务维护详情
     *
     * @param id
     * @return
     */
    @GetMapping("/details")
    public AjaxResult getServiceMaintenanceDetail(Long id) {
        return AjaxResult.success(serviceMaintenanceService.getServiceMaintenanceDetail(id));
    }
    /**
     * 新增服务
     */
    @PostMapping("/add")
    public AjaxResult addServiceMaintenance(@RequestBody AddServiceMaintenanceListReq addReq) {
        return toAjax(serviceMaintenanceService.addServiceMaintenance(addReq, getLoginUser()));
    }
    /**
     * 修改服务维护
     */
    @PostMapping("/edit")
    public AjaxResult editServiceMaintenance(@RequestBody UpdateServiceMaintenanceListReq updateReq) {
        return toAjax(serviceMaintenanceService.editServiceMaintenance(updateReq, getLoginUser()));
    }
    /**
     * 删除服务维护
     */
    @PostMapping("/delete")
    public AjaxResult deleteServiceMaintenance(@RequestParam Long id) {
        return toAjax(serviceMaintenanceService.deleteServiceMaintenance(id, getLoginUser()));
    }

    @PostMapping("/distributeProcess")
    public AjaxResult distributeProcess(@RequestBody DistributeServiceProcessReq req) {
        return toAjax(serviceMaintenanceService.distributeProcess(req, getLoginUser()));
    }

    /**
     * 获取服务流程-serviceId
     * @param req
     * @return
     */
    @PostMapping("/getServiceProcessList")
    public AjaxResult getServiceProcessList(@RequestBody GetServiceProcessListReq req) {
        return AjaxResult.success(serviceMaintenanceService.getServiceProcessList(req));
    }

    /**
     * 获取服务信息-productId
     * @param productId
     * @return
     */
    @PostMapping("/getServiceByProductId")
    public AjaxResult getServiceByProductId(@RequestParam Long productId){
        return AjaxResult.success(serviceMaintenanceService.getServiceByProductId(productId));
    }

}