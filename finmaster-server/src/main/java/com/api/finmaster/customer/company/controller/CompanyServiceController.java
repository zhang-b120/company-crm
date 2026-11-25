package com.qykj.finmaster.customer.company.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.page.TableDataInfo;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.customer.company.domain.req.*;
import com.qykj.finmaster.customer.company.service.CompanyServiceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-08-15
 * @Description
 */
@RestController
@RequestMapping("/company")
public class CompanyServiceController extends BaseController {

    @Autowired
    private CompanyServiceServiceImpl companyServiceService;

    /**
     * 查询存在服务公司列表分页
     * @param req
     * @return
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/getCompanyListWithService")
    public TableDataInfo getCompanyListWithService(@RequestBody GetCompanyListWithServiceReq req){
        return getDataTable(companyServiceService.getCompanyListWithService(req));
    }

    /**
     * 查询存在服务公司列表维护分页
     * @param req
     * @return
     */
    @PreAuthorize("@ss.hasPermi('company:record')")
    @PostMapping("/getCompanyRecordListWithService")
    public TableDataInfo getCompanyRecordListWithService(@RequestBody GetCompanyRecordListWithServiceReq req){
        return getDataTable(companyServiceService.getCompanyRecordListWithService(req));
    }

    /**
     * 查询存在服务公司列表续费分页
     * @param req
     * @return
     */
    @PreAuthorize("@ss.hasPermi('company:renewal')")
    @PostMapping("/getCompanyRenewListWithService")
    public TableDataInfo getCompanyRenewListWithService(@RequestBody GetCompanyRenewListWithServiceReq req){
        return getDataTable(companyServiceService.getCompanyRenewListWithService(req));
    }

    /**
     * 查询存在服务公司列表续费分页
     * @param req
     * @return
     */
    @PostMapping("/getCompanyDetailInfo")
    public AjaxResult getCompanyDetailInfo(@RequestBody GetCompanyRenewListWithServiceReq req){
        return AjaxResult.success(companyServiceService.getCompanyRenewListWithService(req));
    }


    /**
     * 添加公司维护记录
     * @param req
     * @return
     */
    @PostMapping("/addCompanyRecord")
    public AjaxResult addCompanyRecord(@RequestPart AddCompanyRecordReq req,
                                       @RequestPart(value = "fileList", required = false) List<MultipartFile> fileList){
        return AjaxResult.success(companyServiceService.addCompanyRecord(req, fileList, getLoginUser()));
    }

    /**
     * 获取公司历史维护记录
     * @param req
     * @return
     */
    @PostMapping("/getCompanyRecordHistoryList")
    public TableDataInfo getCompanyRecordHistoryPage(@RequestBody GetCompanyRecordHistoryPageReq req){
        return getDataTable(companyServiceService.getCompanyRecordHistoryPage(req));
    }

    /**
     * 续费统计：按 company_service.actual_end_date 的年月分组，统计每月【公司数量】【合同金额】。
     * 参数 startMonth/endMonth（YYYY-MM，可空，缺省取当年 01~12），区间内无数据的月份补零。
     */
    @PreAuthorize("@ss.hasPermi('company:renewal:stats')")
    @GetMapping("/renewalStats")
    public AjaxResult renewalStats(@RequestParam(value = "startMonth", required = false) String startMonth,
                                   @RequestParam(value = "endMonth", required = false) String endMonth,
                                   @RequestParam(value = "butlerOwnerUserName", required = false) List<String> butlerOwnerUserName,
                                   @RequestParam(value = "serviceName", required = false) List<String> serviceName){
        return AjaxResult.success(companyServiceService.getRenewalStats(startMonth, endMonth, butlerOwnerUserName, serviceName));
    }

    /**
     * 续费统计下钻明细：按月（YYYY-MM）返回该月到期的公司服务明细（公司名/税号、联系人/电话、管家、起止时间）。
     * 过滤口径与 renewalStats 完全一致，保证明细行数与「服务数量」对齐。
     */
    @PreAuthorize("@ss.hasPermi('company:renewal:stats')")
    @GetMapping("/renewalStatsDetail")
    public AjaxResult renewalStatsDetail(@RequestParam("month") String month,
                                         @RequestParam(value = "butlerOwnerUserName", required = false) List<String> butlerOwnerUserName,
                                         @RequestParam(value = "serviceName", required = false) List<String> serviceName){
        return AjaxResult.success(companyServiceService.getRenewalStatDetail(month, butlerOwnerUserName, serviceName));
    }

    /**
     * 公司服务续费
     * @param req
     * @return
     */
    @PostMapping("/addCompanyRenew")
    public AjaxResult addCompanyRenew(@RequestBody AddCompanyRenewReq req){
        return AjaxResult.success(companyServiceService.addCompanyRenew(req, getLoginUser()));
    }

    /**
     * 下发续费任务（S3-4 6.3.3）
     * 客户部主管在续费列表点"下发任务"，把某个 CompanyService 派给指定管家
     * 入参 body：{ companyServiceId, assigneeUser, assigneeUserName, note }
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/assignRenewalTask")
    public AjaxResult assignRenewalTask(@RequestBody Map<String, Object> body){
        Long companyServiceId = body.get("companyServiceId") != null ? Long.valueOf(body.get("companyServiceId").toString()) : null;
        String assigneeUser = (String) body.get("assigneeUser");
        String assigneeUserName = (String) body.get("assigneeUserName");
        String note = (String) body.get("note");
        return AjaxResult.success(companyServiceService.assignRenewalTask(companyServiceId, assigneeUser, assigneeUserName, note, getLoginUser()));
    }

    /**
     * 我的待办续费任务列表（管家自己看）
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @GetMapping("/myRenewalTasks")
    public AjaxResult myRenewalTasks(){
        return AjaxResult.success(companyServiceService.listMyRenewalTasks(getLoginUser()));
    }

    /**
     * 取消续费任务派发（清空 pending 字段）
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/cancelRenewalTask")
    public AjaxResult cancelRenewalTask(@RequestParam Long companyServiceId){
        return AjaxResult.success(companyServiceService.cancelRenewalTask(companyServiceId, getLoginUser()));
    }

    /**
     * 获取公司服务续费记录
     * @param companyServiceId
     * @return
     */
    @PostMapping("/getCompanyRenewHistoryList")
    public AjaxResult getCompanyRenewHistoryList(@RequestParam Long companyServiceId){
        return AjaxResult.success(companyServiceService.getCompanyRenewHistoryList(companyServiceId));
    }

    /**
     * 获取公司服务- companyId
     * @param companyId
     * @return
     */
    @PostMapping("/getCompanyServiceByCompany")
    public AjaxResult getCompanyServiceByCompany(@RequestParam Long companyId){
        return AjaxResult.success(companyServiceService.getCompanyServiceByCompany(companyId));
    }

    /**
     * 获取公司服务订单- companyId
     * @param companyId
     * @return
     */
    @PostMapping("/getCompanyServiceOrderByCompany")
    public AjaxResult getCompanyServiceOrderByCompany(@RequestParam Long companyId){
        return AjaxResult.success(companyServiceService.getCompanyServiceOrderByCompany(companyId));
    }

    /**
     * 获取公司服务所属客户- companyId
     * @param companyId
     * @return
     */
    @PostMapping("/getCompanyServiceCustomerByCompany")
    public AjaxResult getCompanyServiceCustomerByCompany(@RequestParam Long companyId){
        return AjaxResult.success(companyServiceService.getCompanyServiceCustomerByCompany(companyId));
    }

    /**
     * 获取公司服务所属公司- companyId
     * @param companyId
     * @return
     */
    @PostMapping("/getCompanyServiceCompany")
    public AjaxResult getCompanyServiceCompany(@RequestParam Long companyId){
        return AjaxResult.success(companyServiceService.getCompanyServiceCompany(companyId));
    }

    /**
     * 获取银行流水上传统计
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/getBankStatementStats")
    public AjaxResult getBankStatementStats(@RequestBody GetCompanyListWithServiceReq req){
        Map<String, Object> stats = companyServiceService.getBankStatementStats(req);
        return AjaxResult.success(stats);
    }

    /**
     * 修改是否记账户
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/updateBookkeeping")
    public AjaxResult updateBookkeeping(@RequestParam Long companyId, @RequestParam Integer isBookkeeping){
        companyServiceService.updateBookkeeping(companyId, isBookkeeping);
        return AjaxResult.success();
    }

    /**
     * 修改双免个体标记
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/updateDoubleExemption")
    public AjaxResult updateDoubleExemption(@RequestParam Long companyId, @RequestParam Integer isDoubleExemption){
        companyServiceService.updateDoubleExemption(companyId, isDoubleExemption);
        return AjaxResult.success();
    }

    /**
     * 修改是否个体工商户
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/updateIndividual")
    public AjaxResult updateIndividual(@RequestParam Long companyId, @RequestParam Integer isIndividual){
        companyServiceService.updateIndividual(companyId, isIndividual);
        return AjaxResult.success();
    }

}