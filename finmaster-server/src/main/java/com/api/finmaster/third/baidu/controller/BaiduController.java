package com.qykj.finmaster.third.baidu.controller;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.third.baidu.service.BaiduServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author zhb
 * @Date 2025-09-10
 * @Description
 */
@RestController
@RequestMapping("/ua/baidu/ocr")
public class BaiduController {

    @Autowired
    private BaiduServiceImpl baiduService;

    @PermitAll
    @PostMapping("/getBasicGeneral")
    public AjaxResult getBasicGeneral(@RequestParam String filePath){
        return AjaxResult.success(baiduService.getBasicGeneral(filePath));
    }
}