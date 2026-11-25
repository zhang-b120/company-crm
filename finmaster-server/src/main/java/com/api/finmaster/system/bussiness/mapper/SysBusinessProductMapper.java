package com.qykj.finmaster.system.bussiness.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.bussiness.domain.SysBusinessProduct;
import com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysBusinessProductMapper extends BaseMapper<SysBusinessProduct> {

    /**
     * 根据业务ID查询关联产品
     */
    List<GetBusinessProductResp> getProductListByBusinessId(@Param("businessId") Long businessId);

    /**
     * 根据多个业务ID查询关联产品
     */
    List<GetBusinessProductResp> getProductListByBusinessIdList(@Param("businessIdList") List<Long> businessIdList);
}

