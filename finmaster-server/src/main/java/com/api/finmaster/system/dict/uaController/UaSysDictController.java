package com.qykj.finmaster.system.dict.uaController;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.core.domain.common.CodeEntity;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author zhb
 * @Date 2025-07-01
 * @Description
 */
@RestController
@RequestMapping("/ua/system/dict")
public class UaSysDictController extends BaseController {
    @Autowired
    private SysDictDataServiceImpl sysDictDataService;

    @PostMapping("/uaGetDictDataListByTypeCode")
    @PermitAll
    public AjaxResult uaGetDictDataListByTypeCode(@RequestBody CodeEntity req){
        return AjaxResult.success(sysDictDataService.getDictDataListByTypeCode(req.getCode()));
    }

    @PostMapping("/uaGetDictDataListByTypeCodeList")
    @PermitAll
    public AjaxResult uaGetDictDataListByTypeCodeList(@RequestBody CodeEntity req){
        return AjaxResult.success(sysDictDataService.uaGetDictDataListByTypeCodeList(req.getCode()));
    }

}