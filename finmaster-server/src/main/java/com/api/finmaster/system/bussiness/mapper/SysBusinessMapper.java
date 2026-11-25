package com.qykj.finmaster.system.bussiness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.bussiness.domain.SysBusiness;
import com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SysBusinessMapper extends BaseMapper<SysBusiness> {

    List<GetBusinessListResp> getBusinessList(@Param("params") Map<String, Object> params);
}

