package com.qykj.finmaster.payment.manager;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.base.IManager;
import com.qykj.finmaster.payment.domain.PaymentConfig;
import com.qykj.finmaster.payment.mapper.PaymentConfigMapper;
import org.springframework.stereotype.Service;

/**
 * 支付配置Manager
 */
public interface PaymentConfigManager extends IManager<PaymentConfigMapper, PaymentConfig> {
} 