package com.qykj.finmaster.history.mapper;

import com.qykj.finmaster.history.HistoryCompanyServiceDateRange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 历史数据场景下，公司服务与业务关联的查询，仅用于一次性生成历史合同
 */
@Mapper
public interface HistoryCompanyServiceMapper {

    /**
     * 根据公司 + 业务（通过业务关联的产品）获取公司服务的生效/实际截止日期范围
     *
     * @param companyId 公司ID
     * @param businessId 业务ID（sys_business.id）
     * @param tenantId 租户ID，用于过滤业务产品关联
     * @return 公司服务日期范围，找不到则返回 null
     */
    HistoryCompanyServiceDateRange selectServiceDateRangeByCompanyAndBusiness(@Param("companyId") Long companyId,
                                                                              @Param("businessId") Long businessId,
                                                                              @Param("tenantId") Long tenantId);
}

