package com.qykj.finmaster.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.domain.OrderDetail;
import com.qykj.finmaster.order.domain.resp.GetOrderListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 订单Mapper接口
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {
    /**
     * 查询订单
     *
     * @param id 订单ID
     * @return 订单
     */
    List<Order> selectOrderById(Long id);

    List<Order> selectOrder(Map<String,Object> map);

    /**
     * 查询订单列表
     *
     * @param
     * @return 订单集合
     */
    List<GetOrderListResp> selectOrderList(@Param("params") Map<String,Object> map);

    /**
     * 修改订单
     *
     * @param order 订单
     * @return 结果
     */
    int updateOrder(Order order);

    /**
     * 删除订单
     *
     * @param id 订单ID
     * @return 结果
     */
    int deleteOrderById(Long id);

    /**
     * 批量删除订单
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    int deleteOrderByIds(Long[] ids);

    /**
     * 更新订单状态
     *
     * @param id 订单ID
     * @param status 订单状态
     * @return 结果
     */
    int updateOrderStatus(Long id, String status);

    /**
     * 更新订单明细的合同日期
     *
     * @param orderDetail 订单明细信息
     * @return 结果
     */
    int updateOrderDetail(OrderDetail orderDetail);
}
