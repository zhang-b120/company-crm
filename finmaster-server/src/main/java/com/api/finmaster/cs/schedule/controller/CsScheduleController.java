package com.qykj.finmaster.cs.schedule.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.cs.schedule.domain.req.GetAvailableSlotsReq;
import com.qykj.finmaster.cs.schedule.domain.req.SaveScheduleConfigReq;
import com.qykj.finmaster.cs.schedule.service.CsScheduleServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cs/schedule")
public class CsScheduleController extends BaseController {

    @Autowired
    private CsScheduleServiceImpl scheduleService;

    /** 新增排班配置（自动生成号源） */
    @PostMapping("/config")
    public AjaxResult saveConfig(@RequestBody SaveScheduleConfigReq req) {
        return toAjax(scheduleService.saveScheduleConfig(req, getTenantId(), getLoginUser()));
    }

    /** 排班配置分页列表 */
    @GetMapping("/config/list")
    public TableDataInfo configList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.qykj.finmaster.cs.schedule.domain.CsScheduleConfig> page =
                scheduleService.getConfigPage(pageNum, pageSize, getTenantId());
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(page.getRecords());
        rsp.setTotal(page.getTotal());
        return rsp;
    }

    /** 删除排班配置 */
    @DeleteMapping("/config/{id}")
    public AjaxResult deleteConfig(@PathVariable Long id) {
        return toAjax(scheduleService.deleteScheduleConfig(id));
    }

    /** 查询某日可用号源（转任务单选班次使用） */
    @PostMapping("/slots/available")
    public AjaxResult getAvailableSlots(@RequestBody GetAvailableSlotsReq req) {
        return AjaxResult.success(scheduleService.getAvailableSlots(req, getTenantId()));
    }
}
