package com.qykj.finmaster.customer.customerInfo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.domain.resp.GetCustomerInfoPageListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 客户信息Mapper接口
 */
@Mapper
public interface CustomerInfoMapper extends BaseMapper<CustomerInfo> {
    /**
     * 查询客户信息
     */
    CustomerInfo selectCustomerInfoById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 查询客户信息列表
     */
    List<GetCustomerInfoPageListResp> selectCustomerInfoList(@Param("params") Map<String, Object> map);

    /**
     * 新增客户信息
     */
    int insertCustomerInfo(@Param("params") Map<String, Object> map);

    /**
     * 修改客户信息
     */
    int updateCustomerInfo(CustomerInfo customerInfo);

    /**
     * 删除客户信息
     */
    int deleteCustomerInfoById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 批量删除客户信息
     */
    int deleteCustomerInfoByIds(@Param("ids") Long[] ids, @Param("tenantId") Long tenantId);

    List<CustomerInfo> getOutCustomerList(@Param("params") Map<String, Object> params);
}
