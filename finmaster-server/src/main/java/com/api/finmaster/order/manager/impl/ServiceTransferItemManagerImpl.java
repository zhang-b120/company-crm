package com.qykj.finmaster.order.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.order.domain.ServiceTransferItem;
import com.qykj.finmaster.order.manager.ServiceTransferItemManager;
import com.qykj.finmaster.order.mapper.ServiceTransferItemMapper;
import org.springframework.stereotype.Service;

@Service
public class ServiceTransferItemManagerImpl
        extends ManagerImpl<ServiceTransferItemMapper, ServiceTransferItem>
        implements ServiceTransferItemManager {
}
