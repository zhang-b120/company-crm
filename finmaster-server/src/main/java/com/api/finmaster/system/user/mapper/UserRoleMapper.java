package com.qykj.finmaster.system.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.user.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 * 负责用户相关数据库操作
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
