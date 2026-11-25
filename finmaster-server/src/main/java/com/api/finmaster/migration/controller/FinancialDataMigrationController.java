package com.qykj.finmaster.migration.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.migration.service.FinancialDataMigrationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 财务数据迁移控制器
 * @Author zhb
 * @Date 2025-07-14
 * @Description
 */
@RestController
@RequestMapping("/migration")
public class FinancialDataMigrationController extends BaseController {

    @Autowired
    private FinancialDataMigrationServiceImpl financialDataMigrationService;

    /**
     * 数据迁移
     * @param companyId
     * @return
     */
    @PostMapping("/migrationData")
    public AjaxResult migrationData(@RequestBody Long companyId){
        return AjaxResult.success(financialDataMigrationService.migrationData(companyId));
    }
}
