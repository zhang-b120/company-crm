package com.qykj.finmaster.registerAddress.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.registerAddress.domain.RegisterAddress;
import com.qykj.finmaster.registerAddress.domain.resp.GetRegisterAddressListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 注册地址Mapper接口
 */
@Mapper
public interface RegisterAddressMapper extends BaseMapper<RegisterAddress> {

    /**
     * 查询注册地址列表
     */
    List<GetRegisterAddressListResp> getRegisterAddressList(@Param("params") Map<String, Object> params);
}
