package com.qykj.finmaster.order.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.order.domain.OrderDetail;
import com.qykj.finmaster.order.manager.OrderDetailManager;
import com.qykj.finmaster.order.mapper.OrderDetailMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-06-17
 * @Description
 */
@Service
public class OrderDetailManagerImpl extends ManagerImpl<OrderDetailMapper, OrderDetail> implements OrderDetailManager {
}