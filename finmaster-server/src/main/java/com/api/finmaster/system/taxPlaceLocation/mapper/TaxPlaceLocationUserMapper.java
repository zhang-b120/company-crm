package com.qykj.finmaster.system.taxPlaceLocation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.taxPlaceLocation.entity.TaxPlaceLocationUser;
import com.qykj.finmaster.system.taxPlaceLocation.entity.dto.GetTaxPlaceLocationUserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaxPlaceLocationUserMapper extends BaseMapper<TaxPlaceLocationUser> {

    GetTaxPlaceLocationUserDto getUserByProvince(@Param("province") String province);
}
