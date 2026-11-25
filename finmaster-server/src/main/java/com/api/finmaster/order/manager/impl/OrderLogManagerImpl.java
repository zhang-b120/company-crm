package com.qykj.finmaster.order.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.order.domain.OrderLog;
import com.qykj.finmaster.order.manager.OrderLogManager;
import com.qykj.finmaster.order.mapper.OrderLogMapper;
import org.springframework.stereotype.Service;

@Service
public class OrderLogManagerImpl  extends ManagerImpl<OrderLogMapper, OrderLog> implements OrderLogManager {

}
