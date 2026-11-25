package com.qykj.finmaster.task.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.task.domain.req.*;
import com.qykj.finmaster.task.service.CommonServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/commonTask")
public class CommonTaskController extends BaseController {

    @Autowired
    private CommonServiceImpl commonService;

    /**
     * 分页查询
     * @param req
     * @return
     */
    @PostMapping("/list")
    public TableDataInfo getCommonTaskPage(@RequestBody GetCommonTaskPageReq req){
        return getDataTable(commonService.getCommonTaskPage(req));

    }

    /**
     * 添加任务
     * @param req
     * @return
     */
    @PostMapping("/addCommonTask")
    public AjaxResult addCommonTask(@RequestBody AddCommonTaskReq req){
        return toAjax(commonService.addCommonTask(req, getTenantId(), getLoginUser()));
    }

    /**
     * 删除任务
     * @param id
     * @return
     */
    @PostMapping("/deleteCommonTask")
    public AjaxResult deleteCommonTask(@RequestParam Long id){
        return toAjax(commonService.deleteCommonTask(id, getTenantId(), getLoginUser()));
    }

    /**
     * 修改任务
     * @param req
     * @return
     */
    @PostMapping("/updateCommonTask")
    public AjaxResult updateCommonTask(@RequestBody UpdateCommonTaskReq req){
        return toAjax(commonService.updateCommonTask(req, getLoginUser()));
    }

    /**
     * 查看任务详情
     * @param id
     * @return
     */
    @PostMapping("/getCommonTaskInfo")
    public AjaxResult getCommonTaskInfo(@RequestParam Long id){
        return AjaxResult.success(commonService.getCommonTaskInfo(id));
    }

    /**
     * 分配负责人
     * @return
     */
    @PostMapping("/distributeOwnerUser")
    public AjaxResult distributeOwnerUser(@RequestBody DistributeTaskOwnerUserReq req){
        return toAjax(commonService.distributeOwnerUser(req, getLoginUser()));
    }

    /**
     * 更换任务负责人
     * @param req
     * @return
     */
    @PostMapping("/changeTaskOwnerUser")
    public AjaxResult changeTaskOwnerUser(@RequestBody TaskChangeOwnerUserReq req){
        return toAjax(commonService.changeTaskOwnerUser(req, getLoginUser()));
    }

    /**
     * 开始任务
     * @param id
     * @return
     */
    @PostMapping("/startTask")
    public AjaxResult startTask(@RequestParam Long id){
        return toAjax(commonService.startTask(id, getLoginUser()));
    }

    /**
     * 执行任务流程
     * @param req
     * @return
     */
    @PostMapping(value = "/executeTaskDetail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult executeTaskDetail(@RequestPart("req") ExecuteTaskDetailReq req,
                                        @RequestPart(value = "file", required = false) List<MultipartFile> fileList){
        return toAjax(commonService.executeTaskDetail(req, fileList, getLoginUser()));
    }

    /**
     * 任务挂起
     * @param id
     * @return
     */
    @PostMapping("/putUpTask")
    public AjaxResult putUpTask(@RequestBody PutupTaskReq req){
        return toAjax(commonService.putUpTask(req, getLoginUser()));
    }

    /**
     * 任务取消
     * @param id
     * @return
     */
    @PostMapping("/cancelTask")
    public AjaxResult cancelTask(@RequestBody CancelTaskReq req){
        return toAjax(commonService.cancelTask(req, getLoginUser()));
    }

    /**
     * 任务恢复
     * @param id
     * @return
     */
    @PostMapping("/restoreTask")
    public AjaxResult restoreTask(@RequestParam Long id){
        return toAjax(commonService.restoreTask(id, getLoginUser()));
    }

    /**
     * 获取待执行任务信息（任务层面）
     * @return
     */
    @PostMapping("/getCommonTaskDealInfo")
    public AjaxResult getCommonTaskDealInfo(){
        return AjaxResult.success(commonService.getCommonTaskDealInfo(getLoginUser()));
    }

    /**
     * 任务文件删除
     * @param req
     * @return
     */
    @PostMapping("/getCommonTaskFileList")
    @PermitAll
    public AjaxResult getCommonTaskFileList(@RequestBody GetCommonTaskFileListReq req){
        return AjaxResult.success(commonService.getCommonTaskFileList(req));
    }

    /**
     * 保存账套初始化数据，以excel文件进行保存。
     * @param req
     * @return
     */
    @PostMapping("/saveFinancialInitFile")
    public AjaxResult saveFinancialInitFile(@RequestBody List<SubjectReq> req){
        return AjaxResult.success(commonService.saveFinancialInitFile(req));
    }
}
