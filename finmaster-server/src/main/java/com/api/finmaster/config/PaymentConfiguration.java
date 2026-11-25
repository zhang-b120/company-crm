package com.qykj.finmaster.config;

import cn.hutool.core.collection.CollUtil;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.payment.domain.PaymentConfig;
import com.qykj.finmaster.payment.manager.impl.PaymentConfigManagerImpl;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @Author zhb
 * @Date 2025-06-12
 * @Description
 */
@Component
@Slf4j
public class PaymentConfiguration {

    @Autowired
    private PaymentConfigManagerImpl paymentConfigManager;

    private final Map<String, Config> configMap = new ConcurrentHashMap<>();
    private final Map<String, PaymentConfig> paymentConfigMap = new ConcurrentHashMap<>();
    private final Map<String, NotificationParser> notificationParserMap = new ConcurrentHashMap<>();

    /**
     * 初始化config
     */
    @PostConstruct
    public void init() {
        // 从数据库加载配置
        List<PaymentConfig> paymentConfigList = paymentConfigManager.lambdaQuery()
                .eq(PaymentConfig::getEnabled, GlobalConstant.Number.ONE_INT)
                .list();
        if (CollUtil.isEmpty(paymentConfigList)) {
            log.error("未找到有效的支付配置!");
        }
        paymentConfigList.forEach(item -> {
            createConfig(item);
            createPaymentConfig(item);
            createNotificationParser(item);
        });
    }

    /**
     * 初始化configMap
     * @param paymentConfig
     */
    private void createConfig(PaymentConfig paymentConfig) {
        if(ObjectUtils.isEmpty(paymentConfig)){
            return;
        }
        Config config = new RSAAutoCertificateConfig.Builder()
                .merchantId(paymentConfig.getMchId())
                .privateKeyFromPath(paymentConfig.getPrivateKeyPath())
                .merchantSerialNumber(paymentConfig.getCertSerialNumber())
                .apiV3Key(paymentConfig.getPrivateKeyV3())
                .build();
        configMap.put(paymentConfig.getPaymentName(), config);
    }

    /**
     * 初始化paymentConfigMap
     * @param paymentConfig
     */
    private void createPaymentConfig(PaymentConfig paymentConfig) {
        if(ObjectUtils.isEmpty(paymentConfig)){
            return;
        }
        paymentConfigMap.put(paymentConfig.getPaymentName(), paymentConfig);
    }

    /**
     * 初始化NotificationParser
     * @param paymentConfig
     */
    private void createNotificationParser(PaymentConfig paymentConfig) {
        if(ObjectUtils.isEmpty(paymentConfig)){
            return;
        }
        if(ObjectUtils.isEmpty(paymentConfig)){
            return;
        }
        NotificationConfig config = new RSAAutoCertificateConfig.Builder()
                .merchantId(paymentConfig.getMchId())
                .privateKeyFromPath(paymentConfig.getPrivateKeyPath())
                .merchantSerialNumber(paymentConfig.getCertSerialNumber())
                .apiV3Key(paymentConfig.getPrivateKeyV3())
                .build();
        notificationParserMap.put(paymentConfig.getPaymentName(), new NotificationParser(config));
    }

    /**
     * 获取config
     * @param paymentName
     * @return
     */
    public Config getConfig(String paymentName){
        if(MapUtils.isEmpty(configMap)){
            return null;
        }
        return configMap.get(paymentName);
    }

    /**
     * 获取paymentConfig
     * @param paymentName
     * @return
     */
    public PaymentConfig getPaymentConfig(String paymentName){
        if(MapUtils.isEmpty(configMap)){
            return null;
        }
        return paymentConfigMap.get(paymentName);
    }

    /**
     * 获取NotificationParser
     * @param paymentName
     * @return
     */
    public NotificationParser getNotificationParser(String paymentName){
        if(MapUtils.isEmpty(configMap)){
            return null;
        }
        return notificationParserMap.get(paymentName);
    }

    /**
     * 刷新配置
     */
    @Scheduled(initialDelay = GlobalConstant.DAY_SCALE, fixedRate = GlobalConstant.DAY_SCALE)
    public void refreshConfigs() {
        this.init();
    }
}
