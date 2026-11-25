package com.qykj.finmaster.order.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.order.domain.OrderLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单日志Mapper接口
 */
@Mapper
public interface OrderLogMapper extends BaseMapper<OrderLog> {
    /**
     * 修改订单日志
     */
    public int updateOrderLog(OrderLog orderLog);

    /**
     * 删除订单日志
     */
    public int deleteOrderLogById(Long id);

}
