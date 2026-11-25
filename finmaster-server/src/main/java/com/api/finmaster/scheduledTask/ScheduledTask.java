package com.qykj.finmaster.scheduledTask;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.constansts.WxConstants;
import com.qykj.finmaster.payment.domain.PaymentRecord;
import com.qykj.finmaster.payment.domain.req.JsapiQueryReq;
import com.qykj.finmaster.payment.manager.PaymentRecordManager;
import com.qykj.finmaster.payment.service.PaymentServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author zhb
 * @Date 2025-07-04
 * @Description
 */
@Component
@Slf4j
public class ScheduledTask {

    @Autowired
    private PaymentServiceImpl paymentService;

    @Autowired
    private PaymentRecordManager paymentRecordManager;

    // 每6分钟执行一次
    @Scheduled(fixedRate = GlobalConstant.SIX_MINUTE_SCALE)
    public void executeQueryTask() {
        List<PaymentRecord> paymentRecordList = paymentRecordManager.lambdaQuery()
                .in(PaymentRecord::getPaymentStatus, Arrays.asList(DictConstants.PAY_STATUS_START, DictConstants.PAY_STATUS_WAIT))
                .list();
        System.out.println("微信支付订单查询开始：············");
        if(CollUtil.isNotEmpty(paymentRecordList)){
            List<String> orderNoList = paymentRecordList.stream().map(PaymentRecord::getOrderNo).collect(Collectors.toList());
            System.out.println("微信支付订单查询：" +  orderNoList);
            orderNoList.forEach(item -> {
                JsapiQueryReq req = new JsapiQueryReq();
                req.setPaymentName(WxConstants.DEFAULT_PAYMENT_NAME);
                req.setOrderNo(item);
                paymentService.wxJsapiQuery(req);
            });
        }
        System.out.println("微信支付订单查询结束：············");
    }
}
