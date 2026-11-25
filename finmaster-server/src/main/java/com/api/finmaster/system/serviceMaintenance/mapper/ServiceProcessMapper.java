package com.qykj.finmaster.system.serviceMaintenance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceProcess;
import com.qykj.finmaster.system.serviceMaintenance.domain.resp.GetServiceProcessListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;


@Mapper
public interface ServiceProcessMapper extends BaseMapper<ServiceProcess>{

    List<GetServiceProcessListResp> selectServiceProcessList(@Param("params") Map<String, Object> params);

    List<ServiceProcess> getListByServiceId(@Param("id") Long id);
}
