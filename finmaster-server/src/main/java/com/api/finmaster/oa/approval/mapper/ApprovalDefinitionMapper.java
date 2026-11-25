package com.qykj.finmaster.oa.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.oa.approval.domain.ApprovalDefinition;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审批流程定义Mapper接口
 */
@Mapper
public interface ApprovalDefinitionMapper extends BaseMapper<ApprovalDefinition> {
}
