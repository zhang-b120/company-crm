package com.qykj.finmaster.system.serviceMaintenance.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.system.serviceMaintenance.domain.req.AddServiceProcessListReq;
import com.qykj.finmaster.system.serviceMaintenance.domain.req.GetServiceMProcessListReq;
import com.qykj.finmaster.system.serviceMaintenance.domain.req.UpdateServiceProcessListReq;
import com.qykj.finmaster.system.serviceMaintenance.service.ServiceProcessServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/serviceProcess")
public class ServiceProcessController extends BaseController {
    @Autowired
    private ServiceProcessServiceImpl serviceProcessService;

    /**
     * 服务流程列表查询
     * @param req
     * @return
     */
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GetServiceMProcessListReq req){
        return getDataTable(serviceProcessService.list(req));
    }

    /**
     * 查询服务流程详情
     * @param id
     * @return
     */
    @PostMapping("/getInfo")
    public AjaxResult getInfo(@RequestParam Long id){
        return AjaxResult.success(serviceProcessService.getInfo(id));
    }

    /**
     * 添加服务流程
     * @param req
     * @return
     */
    @PostMapping("/add")
    public AjaxResult addServiceProcess(@RequestBody AddServiceProcessListReq req){
        return toAjax(serviceProcessService.addServiceProcess(req, getLoginUser()));
    }

    /**
     * 修改服务流程
     * @param req
     * @return
     */
    @PostMapping("/edit")
    public AjaxResult updateServiceProcess(@RequestBody UpdateServiceProcessListReq req){
        return toAjax(serviceProcessService.updateServiceProcess(req, getLoginUser()));
    }

    /**
     * 删除服务流程
     * @param id
     * @return
     */
    @PostMapping("/delete")
    public AjaxResult deleteServiceProcess(@RequestParam Long id){
        return toAjax(serviceProcessService.deleteServiceProcess(id, getLoginUser()));
    }


}


