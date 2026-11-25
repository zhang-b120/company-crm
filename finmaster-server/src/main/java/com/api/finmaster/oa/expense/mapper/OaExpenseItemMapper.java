package com.qykj.finmaster.oa.expense.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.oa.expense.domain.OaExpenseItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报销明细Mapper接口
 */
@Mapper
public interface OaExpenseItemMapper extends BaseMapper<OaExpenseItem> {
}
