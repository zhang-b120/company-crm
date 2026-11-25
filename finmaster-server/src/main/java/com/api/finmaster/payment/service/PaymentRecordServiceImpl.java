package com.qykj.finmaster.payment.service;

import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.WxConstants;
import com.qykj.finmaster.payment.domain.PaymentRecord;
import com.qykj.finmaster.payment.manager.PaymentRecordManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * 支付记录Service实现类
 */
@Service
public class PaymentRecordServiceImpl {

    @Autowired
    private PaymentRecordManager paymentRecordManager;

    @Transactional
    public Boolean createPaymentRecord(String orderNumber, Long paymentConfigId, Integer paymentAmount){
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderNo(orderNumber);
        paymentRecord.setPaymentConfigId(paymentConfigId);
        paymentRecord.setPaymentStatus(DictConstants.PAY_STATUS_START);
        paymentRecord.setNotifyStatus(DictConstants.NOTIFY_STATUS_START);
        paymentRecord.setPrepayTime(LocalDateTime.now());
        paymentRecord.setTimeOutInterval(WxConstants.WX_PAY_TIME_OUT_INTERVAL);
        paymentRecord.setPaymentAmount(paymentAmount);
        paymentRecord.setCreateTime(new Date());
        paymentRecord.setUpdateTime(new Date());
        paymentRecordManager.save(paymentRecord);
        return Boolean.TRUE;
    }
} 