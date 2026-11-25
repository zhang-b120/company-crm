package com.qykj.finmaster.order.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.manager.OrderManager;
import com.qykj.finmaster.order.mapper.OrderMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-06-17
 * @Description
 */
@Service
public class OrderManagerImpl extends ManagerImpl<OrderMapper, Order> implements OrderManager {
}