package com.qykj.finmaster.history.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 历史合同相关查询 Mapper，仅用于一次性生成历史合同
 */
@Mapper
public interface HistoryContractMapper {

    /**
     * 判断是否已存在某公司+业务+合同类型的“历史订单批量生成”合同
     */
    int countHistoryBatchContract(@Param("tenantId") Long tenantId,
                                  @Param("companyId") Long companyId,
                                  @Param("businessId") Long businessId,
                                  @Param("contractType") String contractType,
                                  @Param("remark") String remark);
}

