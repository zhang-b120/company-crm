package com.qykj.finmaster.data.domain.req;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-12-15
 * @Description
 */
@Data
public class GetSaleLeadListReq {

    /**
     * 开始时间
     */
    private LocalDateTime start;

    /**
     * 结束时间
     */
    private LocalDateTime end;

    /**
     * 线索来源（1小程序注册 2引流 3抖音 4小红书 5官网 6客户介绍 small_produce小程序注册）
     */
    private String source;
}