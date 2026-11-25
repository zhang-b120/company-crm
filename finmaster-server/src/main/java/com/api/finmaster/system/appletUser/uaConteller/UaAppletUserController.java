package com.qykj.finmaster.system.appletUser.uaConteller;

import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.system.appletUser.domain.req.AddAppletUserRecommendReq;
import com.qykj.finmaster.system.appletUser.domain.req.AddAppletUserReq;
import com.qykj.finmaster.system.appletUser.domain.req.GetAppletUserInfoReq;
import com.qykj.finmaster.system.appletUser.service.AppletUserServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@RestController
@RequestMapping("/ua/appletUser")
public class UaAppletUserController {

    @Autowired
    private AppletUserServiceImpl appletUserService;

    /**
     * 小程序分享注册用户账户信息
     * @return
     */
    @PostMapping("/addAppletUserByShare")
    @PermitAll
    public AjaxResult addAppletUserByShare(@RequestBody AddAppletUserReq req){
        return AjaxResult.success(appletUserService.addAppletUserByShare(req));
    }


    /**
     * 获取登录用户基本信息
     * @return
     */
    @PostMapping("/getAppletUserInfo")
    @PermitAll
    public AjaxResult getAppletUserInfo(@RequestBody GetAppletUserInfoReq req){
        return AjaxResult.success(appletUserService.getAppletUserInfo(req));
    }


    /**
     * 小程序推荐用户
     * @return
     */
    @PostMapping("/addAppletUserRecommend")
    @PermitAll
    public AjaxResult addAppletUserRecommend(@RequestBody AddAppletUserRecommendReq req){
        return AjaxResult.success(appletUserService.addAppletUserRecommend(req));
    }

    /**
     * 获取推荐有效客户信息
     * @param openId
     * @return
     */
    @PostMapping("/getEffectiveUserInfo")
    @PermitAll
    public AjaxResult getEffectiveUserInfo(@RequestParam String openId){
        return AjaxResult.success(appletUserService.getEffectiveUserInfo(openId));
    }

}