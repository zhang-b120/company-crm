package com.qykj.finmaster.system.appletUser.controller;

import com.qykj.finmaster.system.appletUser.service.AppletUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序用户控制器
 */
@RestController
@RequestMapping("/appletUser")
public class AppletUserController {

    @Autowired
    private AppletUserServiceImpl appletUserService;

}