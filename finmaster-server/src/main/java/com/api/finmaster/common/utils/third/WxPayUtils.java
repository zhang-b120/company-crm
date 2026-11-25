package com.qykj.finmaster.common.utils.third;


import com.qykj.finmaster.payment.domain.PaymentConfig;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.RSAConfig;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;

/**
 * @Author zhb
 * @Date 2025-06-11
 * @Description
 */
public class WxPayUtils {

    public static Config getConfig(PaymentConfig paymentConfig){
        Config config =
                new RSAConfig.Builder()
                        .merchantId(paymentConfig.getMchId())
                        .privateKeyFromPath(paymentConfig.getPrivateKeyPath())
                        .merchantSerialNumber(paymentConfig.getCertSerialNumber())
                        //.apiV3Key(paymentConfig.getPrivateKeyV3())
                        .wechatPayCertificatesFromPath(paymentConfig.getCertPath())
                        .build();
        return config;
    }

    public static void order(Config config){
        JsapiServiceExtension service = new JsapiServiceExtension.Builder().config(config).build();
        PrepayRequest request = new PrepayRequest();

        PrepayWithRequestPaymentResponse response = service.prepayWithRequestPayment(request);
    }
}