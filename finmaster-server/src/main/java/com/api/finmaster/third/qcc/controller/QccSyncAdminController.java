package com.qykj.finmaster.third.qcc.controller;

import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.third.qcc.service.QccSyncAdminServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 一次性管理员接口：批量把 customer_company 送到企查查模糊查询并落库。
 * 使用时将RequestMapping 修改为/admin/qcc
 */
@RestController
@RequestMapping("/testqcc")
public class QccSyncAdminController {

    @Autowired
    private QccSyncAdminServiceImpl qccSyncAdminService;

    @PostMapping("/sync-all")
    public Result<Map<String, Object>> syncAll() {
        return Result.ok(qccSyncAdminService.syncAll());
    }
}
