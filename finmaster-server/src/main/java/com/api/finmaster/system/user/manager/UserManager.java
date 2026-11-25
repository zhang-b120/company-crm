package com.qykj.finmaster.system.user.manager;

import com.qykj.finmaster.common.base.IManager;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.mapper.UserMapper;

/**
 * 用户Manager接口
 * 
 * @Author zhb
 * @Date 2025-01-03
 * @Description 用户管理数据访问层
 */
public interface UserManager extends IManager<UserMapper, User> {
    
} 