package com.qykj.finmaster.config;

import com.baidu.aip.ocr.AipOcr;
import com.qykj.finmaster.common.core.domain.constansts.BaiduConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.third.BaiduUtils;
import com.qykj.finmaster.common.utils.third.resp.GetBaiduAccessTokenResp;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @Author zhb
 * @Date 2025-09-10
 * @Description
 */
@Slf4j
@Component
public class BaiduApiConfig {

    private volatile String accessToken;

    /**
     * 获取百度API AccessToken
     * @return
     */
    public String getBaiduAccessToken() {
        if(StringUtils.isEmpty(accessToken)) {
            initToken();
        }
        return accessToken;
    }

    @PostConstruct
    public void initToken() {
        accessToken = BaiduUtils.getBaiduAiAccessToken().getAccessToken();
    }

    @Scheduled(initialDelay = GlobalConstant.TEN_DAY_SCALE,fixedRate = GlobalConstant.TEN_DAY_SCALE)
    public void scheduledRefreshToken() {
        this.initToken();
    }
}