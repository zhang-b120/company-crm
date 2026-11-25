package com.qykj.finmaster.customer.company.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.company.domain.CustomerCompanyLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerCompanyLogMapper extends BaseMapper<CustomerCompanyLog> {
    /**
     * 查询客户公司操作日志
     */
    public CustomerCompanyLog selectCustomerCompanyLogById(Long id);

    /**
     * 查询客户公司操作日志列表
     * 
     * @param customerCompanyLog 客户公司操作日志信息
     * @return 客户公司操作日志集合
     */
    public List<CustomerCompanyLog> selectCustomerCompanyLogList(CustomerCompanyLog customerCompanyLog);

    /**
     * 新增客户公司操作日志
     * 
     * @param customerCompanyLog 客户公司操作日志信息
     * @return 结果
     */
    public int insertCustomerCompanyLog(CustomerCompanyLog customerCompanyLog);

    /**
     * 修改客户公司操作日志
     */
    public int updateCustomerCompanyLog(CustomerCompanyLog customerCompanyLog);

    /**
     * 删除客户公司操作日志
     */
    public int deleteCustomerCompanyLogById(Long id);

    /**
     * 批量删除客户公司操作日志
     */
    public int deleteCustomerCompanyLogByIds(Long[] ids);
} 