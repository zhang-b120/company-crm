package com.qykj.finmaster.system.appletUser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.appletUser.domain.AppletUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 小程序用户数据访问层
 */
@Mapper
public interface AppletUserMapper extends BaseMapper<AppletUser> {

}