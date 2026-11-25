package com.qykj.finmaster;

import com.baidu.aip.ocr.AipOcr;
import com.qykj.finmaster.common.core.domain.constansts.BaiduConstants;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.qykj.finmaster")
@MapperScan({"com.qykj.finmaster.**.mapper","com.qykj.finmaster.*.*.mapper"})
@EnableScheduling
@EnableAsync
public class FinMasterApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinMasterApplication.class, args);
    }

    @Bean
    public AipOcr getAipOcr() {
        return new AipOcr(BaiduConstants.APP_ID, BaiduConstants.API_KEY, BaiduConstants.SECRET_KEY);
    }
}
