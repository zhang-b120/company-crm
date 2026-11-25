package com.qykj.finmaster.customer.customerInfoLog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.customerInfoLog.domain.CustomerLog;
import com.qykj.finmaster.customer.customerInfoLog.domain.resp.GetCustomerLogPageResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 客户操作日志Mapper接口
 */
@Mapper
public interface CustomerLogMapper extends BaseMapper<CustomerLog> {
    /**
     * 查询客户操作日志
     */
    CustomerLog selectCustomerLogById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 查询客户操作日志列表
     */
    List<GetCustomerLogPageResp> selectCustomerLogList(@Param("params") Map<String, Object> customerLog);

    /**
     * 新增客户操作日志
     */
    int insertCustomerLog(CustomerLog customerLog);

    /**
     * 修改客户操作日志
     */
    int updateCustomerLog(CustomerLog customerLog);

    /**
     * 删除客户操作日志
     */
    int deleteCustomerLogById(@Param("id") Long id);

    /**
     * 批量删除客户操作日志
     */
    int deleteCustomerLogByIds(@Param("ids") Long[] ids);
}
