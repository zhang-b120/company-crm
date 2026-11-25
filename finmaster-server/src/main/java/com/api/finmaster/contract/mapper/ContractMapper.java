package com.qykj.finmaster.contract.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.domain.resp.GetContractListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 合同Mapper接口
 */
@Mapper
public interface ContractMapper extends BaseMapper<Contract> {

    /**
     * 查询合同列表
     */
    List<GetContractListResp> getContractList(@Param("params") Map<String, Object> params);

    /**
     * 按业务类型名称查询有效合同的公司ID集合（用于客户档案过滤）
     */
    List<Long> getCompanyIdsByBusinessNames(@Param("tenantId") Long tenantId,
                                            @Param("businessNames") List<String> businessNames);
}
