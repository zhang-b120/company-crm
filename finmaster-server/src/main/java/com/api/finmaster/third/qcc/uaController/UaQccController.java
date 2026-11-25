package com.qykj.finmaster.third.qcc.uaController;

import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.third.qcc.req.GetQccCompanyInfoReq;
import com.qykj.finmaster.third.qcc.resp.GetQccCompanyInfoResp;
import com.qykj.finmaster.third.qcc.service.QccServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author zhb
 * @Date 2025-05-30
 * @Description
 */
@RestController
@RequestMapping("/ua/qcc")
public class UaQccController {

    @Autowired
    private QccServiceImpl qccService;

    @PostMapping("/getQccCompanyInfo")
    @PermitAll
    public Result<List<GetQccCompanyInfoResp>> getQccCompanyInfo(@RequestBody GetQccCompanyInfoReq req){
        return Result.ok(qccService.getQccCompanyInfo(req));
    }
}