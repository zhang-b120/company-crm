package com.qykj.finmaster.oa.payment.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.payment.domain.OaPayment;
import com.qykj.finmaster.oa.payment.manager.OaPaymentManager;
import com.qykj.finmaster.oa.payment.mapper.OaPaymentMapper;
import org.springframework.stereotype.Service;

/**
 * 付款申请Manager实现
 */
@Service
public class OaPaymentManagerImpl extends ManagerImpl<OaPaymentMapper, OaPayment> implements OaPaymentManager {
}
