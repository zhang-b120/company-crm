package com.qykj.finmaster.system.user.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.manager.UserManager;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import org.springframework.stereotype.Service;

/**
 * 用户Manager实现类
 * 
 * @Author zhb
 * @Date 2025-01-03
 * @Description 用户管理数据访问层实现
 */
@Service
public class UserManagerImpl extends ManagerImpl<UserMapper, User> implements UserManager {
    
} 