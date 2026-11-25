package com.qykj.finmaster.customer.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.company.domain.CompanyRecord;
import com.qykj.finmaster.customer.company.domain.resp.GetCompanyRecordHistoryPageResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CompanyRecordMapper extends BaseMapper<CompanyRecord> {

    List<GetCompanyRecordHistoryPageResp> getCompanyRecordHistoryPage(@Param("params") Map<String, Object> params);
}