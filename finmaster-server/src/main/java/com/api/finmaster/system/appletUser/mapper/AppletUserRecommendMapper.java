package com.qykj.finmaster.system.appletUser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.appletUser.domain.AppletUserRecommend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 小程序用户数据访问层
 */
@Mapper
public interface AppletUserRecommendMapper extends BaseMapper<AppletUserRecommend> {

    List<AppletUserRecommend> getEffectiveUser(@Param("openId") String openId);
}