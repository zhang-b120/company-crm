package com.qykj.finmaster.order.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.order.domain.RefundOrderInfo;
import com.qykj.finmaster.order.manager.RefundOrderInfoManager;
import com.qykj.finmaster.order.mapper.RefundOrderInfoMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-11-20
 * @Description
 */
@Service
public class RefundOrderInfoManagerImpl extends ManagerImpl<RefundOrderInfoMapper, RefundOrderInfo> implements RefundOrderInfoManager {
}