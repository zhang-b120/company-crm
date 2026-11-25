package com.qykj.finmaster.payment.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.payment.domain.PaymentConfig;
import com.qykj.finmaster.payment.manager.PaymentConfigManager;
import com.qykj.finmaster.payment.mapper.PaymentConfigMapper;
import org.springframework.stereotype.Service;

/**
 * 支付配置Manager
 */
@Service
public class PaymentConfigManagerImpl extends ManagerImpl<PaymentConfigMapper, PaymentConfig> implements PaymentConfigManager {
} 