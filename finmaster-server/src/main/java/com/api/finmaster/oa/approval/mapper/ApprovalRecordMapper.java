package com.qykj.finmaster.oa.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.oa.approval.domain.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批记录Mapper接口
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {
}
