package com.qykj.finmaster.third.okcc.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.third.okcc.entity.resp.OutPhoneResp;
import com.qykj.finmaster.third.okcc.service.UserSeatNumberInfoService;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author lz
 * @Date 2025-05-30
 * @Description
 */
@RestController
@RequestMapping("/out/phone")
public class OutPhoneController extends BaseController {

    @Autowired
    private UserSeatNumberInfoService service;

    /**
     * 坐席登录
     * @return
     */
    @PostMapping("/agentLogin")
    @PermitAll
    public Result<OutPhoneResp> agentLogin(){

        return Result.ok(service.agentLogin(getUserId()));
    }
    /**
     * 坐席退出
     * @return
     */
    @PostMapping("/agentLogout")
    @PermitAll
    public Result<OutPhoneResp> agentLogout(){
        return Result.ok(service.agentLogout(getUserId()));
    }

    /**
     * 拨号
     * @param phoneNumber 电话号码
     * @return
     */
    @PostMapping("/callNumber")
    @PermitAll
    public Result<OutPhoneResp> callNumber(@RequestBody Long phoneNumber){
        return Result.ok(service.callNumber(getUserId(),phoneNumber));
    }

    /**
     * 挂断
     * @return
     */
    @PostMapping("/hangupCall")
    @PermitAll
    public Result<OutPhoneResp> hangupCall(){
        return Result.ok(service.hangupCall(getUserId()));
    }

    /**
     * 坐席置忙
     * @return
     */
    @PostMapping("/agentSetBusy")
    @PermitAll
    public Result<OutPhoneResp> agentSetBusy(){
        return Result.ok(service.agentSetBusy(getUserId()));
    }

    /**
     * 坐席置闲
     * @return
     */
    @PostMapping("/agentSetIdle")
    @PermitAll
    public Result<OutPhoneResp> agentSetIdle(){
        return Result.ok(service.agentSetIdle(getUserId()));
    }
}