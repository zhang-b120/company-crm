package com.qykj.finmaster.payment.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.payment.domain.PaymentRecord;
import com.qykj.finmaster.payment.manager.PaymentRecordManager;
import com.qykj.finmaster.payment.mapper.PaymentRecordMapper;
import org.springframework.stereotype.Service;

/**
 * 支付记录Manager
 */
@Service
public class PaymentRecordManagerImpl extends ManagerImpl<PaymentRecordMapper, PaymentRecord> implements PaymentRecordManager {
} 