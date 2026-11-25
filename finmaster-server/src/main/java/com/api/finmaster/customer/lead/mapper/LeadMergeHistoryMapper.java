package com.qykj.finmaster.customer.lead.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.lead.entity.LeadMergeHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 线索合并审计 Mapper
 */
@Mapper
public interface LeadMergeHistoryMapper extends BaseMapper<LeadMergeHistory> {
}
