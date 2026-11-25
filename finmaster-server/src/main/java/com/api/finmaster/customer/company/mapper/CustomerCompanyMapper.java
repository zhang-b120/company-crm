package com.qykj.finmaster.customer.company.mapper;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.domain.resp.GetCustomerCompanyPageResp;
import com.qykj.finmaster.customer.company.domain.resp.ProvinceCountVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CustomerCompanyMapper extends BaseMapper<CustomerCompany> {
    /**
     * 查询客户公司信息
     */
    CustomerCompany selectCustomerCompanyById(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 新增客户公司信息
     */
    int insertCustomerCompany(CustomerCompany customerCompany);

    /**
     * 修改客户公司信息
     */
    int updateCustomerCompany(CustomerCompany customerCompany);

    /**
     * 删除客户公司信息
     */
    int deleteCustomerCompanyById(Long id);

    /**
     * 批量删除客户公司信息
     */
    int deleteCustomerCompanyByIds(Long[] ids);

    /**
     * 查询客户公司信息列表
     * @param params
     * @return
     */
    List<GetCustomerCompanyPageResp> getCustomerCompanyList(@Param("params") Map<String, Object> params);

    /**
     * 大屏：合作中公司总数（DISTINCT tax_number）。
     * 严格口径 = company_status='COOPERATING' 且 存在 company_service(service_id=1) 记账服务；
     * 不分省、不加 deleted（对齐用户权威 SQL）、全量跨租户。
     */
    Long coopCompanyTotal();

    /**
     * 大屏：合作中公司按省份去重税号计数（同口径 + 省份非空 + 倒序），供地图/榜单。
     */
    List<ProvinceCountVo> coopCompanyByProvince();
}