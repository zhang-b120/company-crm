package com.qykj.finmaster.system.serviceMaintenance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceMaintenance;
import com.qykj.finmaster.system.serviceMaintenance.domain.req.GetServiceMaintenanceListReq;
import com.qykj.finmaster.system.serviceMaintenance.domain.resp.GetServiceMaintenanceListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;
@Mapper
public interface ServiceMaintenanceMapper extends BaseMapper<ServiceMaintenance>{
    List<GetServiceMaintenanceListResp> getServiceMaintenanceList(@Param("params")Map<String, Object> params);

    ServiceMaintenance getServiceByProductId(@Param("productId") Long productId);
}
