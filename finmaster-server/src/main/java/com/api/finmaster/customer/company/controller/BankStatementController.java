package com.qykj.finmaster.customer.company.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.customer.company.service.BankStatementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * 银行流水文件管理 Controller
 */
@RestController
@RequestMapping("/company/bankStatement")
public class BankStatementController {

    @Autowired
    private BankStatementService bankStatementService;

    /**
     * 根据统一社会信用代码获取账套ID
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/getAccountSetsId")
    public AjaxResult getAccountSetsId(@RequestParam String creditCode) {
        JSONObject data = bankStatementService.getAccountSetsId(creditCode);
        return AjaxResult.success(data);
    }

    /**
     * 上传银行流水文件
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/upload")
    public AjaxResult upload(@RequestParam("file") MultipartFile file,
                             @RequestParam("accountSetsId") Long accountSetsId,
                             @RequestParam("period") String period,
                             @RequestParam("fileName") String fileName,
                             @RequestParam(value = "businessCode", required = false) String businessCode,
                             @RequestParam("businessName") String businessName,
                             @RequestParam("companyId") Long companyId) {
        JSONObject result = bankStatementService.uploadBankStatement(file, accountSetsId, period, fileName, businessCode, businessName);
        if (result != null && result.getBooleanValue("success")) {
            // 上传成功，更新本地统计
            bankStatementService.incrementUploadCount(companyId, period);
            return AjaxResult.success("上传成功");
        }
        return AjaxResult.error(result != null ? result.getString("msg") : "上传失败");
    }

    /**
     * 银行库列表（CRM 代理财务接口 /OutSystem2CRM/getBankLibList，避免前端直连财务系统）
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @GetMapping("/bankLibList")
    public AjaxResult bankLibList() {
        JSONArray data = bankStatementService.getBankLibList();
        return AjaxResult.success(data);
    }

    /**
     * 批量上传银行流水（多文件 + 多银行）。
     * 财务接口仍是单文件上传，CRM 这里循环调用，按 index 对齐每个文件的 businessName / businessCode。
     * 表单字段：files (多个), businessNames (多个), businessCodes (多个，可空)，其余同 /upload。
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/uploadBatch")
    public AjaxResult uploadBatch(@RequestParam("files") MultipartFile[] files,
                                  @RequestParam("accountSetsId") Long accountSetsId,
                                  @RequestParam("period") String period,
                                  @RequestParam("businessNames") String[] businessNames,
                                  @RequestParam(value = "businessCodes", required = false) String[] businessCodes,
                                  @RequestParam("companyId") Long companyId) {
        if (files == null || files.length == 0) {
            return AjaxResult.error("请至少选择一个文件");
        }
        if (businessNames == null || businessNames.length != files.length) {
            return AjaxResult.error("银行账户数量与文件数量不一致");
        }
        List<String> codeList = businessCodes == null ? null : Arrays.asList(businessCodes);
        JSONObject result = bankStatementService.batchUploadBankStatement(
                Arrays.asList(files), accountSetsId, period,
                Arrays.asList(businessNames), codeList, companyId);
        // 全部成功 / 部分成功 / 全部失败 三种情况都把 summary 透传给前端，由前端按 successCount / failCount 决定提示
        return AjaxResult.success(result);
    }

    /**
     * 查询银行流水文件列表
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/list")
    public AjaxResult list(@RequestBody JSONObject params) {
        Long accountSetsId = params.getLong("accountSetsId");
        String period = params.getString("period");
        int page = params.getIntValue("page") > 0 ? params.getIntValue("page") : 1;
        int pageSize = params.getIntValue("pageSize") > 0 ? params.getIntValue("pageSize") : 20;
        JSONObject result = bankStatementService.listBankStatements(accountSetsId, period, page, pageSize);
        if (result != null && result.getBooleanValue("success")) {
            return AjaxResult.success(result.get("data"));
        }
        return AjaxResult.error(result != null ? result.getString("msg") : "查询失败");
    }

    /**
     * 删除银行流水文件
     */
    @PreAuthorize("@ss.hasPermi('company:list')")
    @PostMapping("/delete")
    public AjaxResult delete(@RequestBody JSONObject params) {
        Long accountSetsId = params.getLong("accountSetsId");
        Long id = params.getLong("id");
        Long companyId = params.getLong("companyId");
        String period = params.getString("period");
        JSONObject result = bankStatementService.deleteBankStatement(accountSetsId, id);
        if (result != null && result.getBooleanValue("success")) {
            // 删除成功，更新本地统计
            if (companyId != null && period != null) {
                bankStatementService.decrementUploadCount(companyId, period);
            }
            return AjaxResult.success("删除成功");
        }
        return AjaxResult.error(result != null ? result.getString("msg") : "删除失败");
    }
}
