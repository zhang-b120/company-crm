package com.qykj.finmaster.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.order.domain.ServiceTransferItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务转移工单·明细行 Mapper
 */
@Mapper
public interface ServiceTransferItemMapper extends BaseMapper<ServiceTransferItem> {
}
