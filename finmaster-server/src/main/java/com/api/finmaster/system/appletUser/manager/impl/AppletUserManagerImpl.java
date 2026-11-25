package com.qykj.finmaster.system.appletUser.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.appletUser.domain.AppletUser;
import com.qykj.finmaster.system.appletUser.manager.AppletUserManager;
import com.qykj.finmaster.system.appletUser.mapper.AppletUserMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@Service
public class AppletUserManagerImpl extends ManagerImpl<AppletUserMapper, AppletUser> implements AppletUserManager {
}