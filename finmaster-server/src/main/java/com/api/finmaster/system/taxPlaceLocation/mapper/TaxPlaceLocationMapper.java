package com.qykj.finmaster.system.taxPlaceLocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.taxPlaceLocation.entity.TaxPlaceLocation;
import com.qykj.finmaster.system.taxPlaceLocation.entity.resp.GetTaxPlaceLocationPageResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TaxPlaceLocationMapper extends BaseMapper<TaxPlaceLocation> {

    List<GetTaxPlaceLocationPageResp> getTaxPlaceLocationPage(@Param("params") Map<String, Object> params);
}
