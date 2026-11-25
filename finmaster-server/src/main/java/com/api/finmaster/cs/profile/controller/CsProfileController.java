package com.qykj.finmaster.cs.profile.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.cs.profile.domain.req.AddCommunicationReq;
import com.qykj.finmaster.cs.profile.domain.req.GetProfilePageReq;
import com.qykj.finmaster.cs.profile.domain.req.SaveProfileReq;
import com.qykj.finmaster.cs.profile.service.CsProfileServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cs/profile")
public class CsProfileController extends BaseController {

    @Autowired
    private CsProfileServiceImpl profileService;

    /** 客户档案分页列表 */
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody GetProfilePageReq req) {
        com.baomidou.mybatisplus.core.metadata.IPage<com.qykj.finmaster.cs.profile.domain.resp.ProfilePageResp> page =
                profileService.getProfilePage(req, getTenantId());
        TableDataInfo rsp = new TableDataInfo();
        rsp.setCode(200);
        rsp.setMsg("查询成功");
        rsp.setRows(page.getRecords());
        rsp.setTotal(page.getTotal());
        return rsp;
    }

    /** 保存/更新客户档案 */
    @PostMapping("/save")
    public AjaxResult save(@RequestBody SaveProfileReq req) {
        return toAjax(profileService.saveProfile(req, getTenantId(), getLoginUser()));
    }

    /** 沟通记录列表 */
    @GetMapping("/communication/list")
    public AjaxResult communicationList(@RequestParam Long companyId) {
        return AjaxResult.success(profileService.getCommunicationList(companyId));
    }

    /** 添加沟通记录 */
    @PostMapping("/communication/add")
    public AjaxResult addCommunication(@RequestBody AddCommunicationReq req) {
        return toAjax(profileService.addCommunication(req, getTenantId(), getLoginUser()));
    }

    /** 删除沟通记录 */
    @DeleteMapping("/communication/{id}")
    public AjaxResult deleteCommunication(@PathVariable Long id) {
        return toAjax(profileService.deleteCommunication(id));
    }
}
