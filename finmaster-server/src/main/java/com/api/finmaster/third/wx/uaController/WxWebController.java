package com.qykj.finmaster.third.wx.uaController;

import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.third.wx.req.GetPhoneDataReq;
import com.qykj.finmaster.third.wx.service.WxWebServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



/**
 * @Author zhb
 * @Date 2025-05-16
 * @Description
 */
@RestController
@RequestMapping("/ua/wx")
public class WxWebController {

    @Autowired
    private WxWebServiceImpl wxWebService;

    @GetMapping("/getOpenId")
    @PermitAll
    public Result<String> getOpenId(String code){
        return Result.ok(wxWebService.getOpenId(code));
    }

    /**
     * 获取手机号
     * @param req
     * @return
     * @throws Exception
     */
    @PostMapping("/getPhoneNum")
    @PermitAll
    public Result<String> getPhoneNum(@RequestBody GetPhoneDataReq req) throws Exception {
        return Result.ok(wxWebService.getPhoneNum(req.getCode(), req.getEncryptedData(), req.getIv()));
    }

    @PostMapping("/getToken")
    public Result<String> getToken(String code) throws Exception {
        return Result.ok(wxWebService.getToken());
    }
}
