package com.qykj.finmaster.customer.lead.entity.req;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-28
 * @Description
 */
@Data
public class AddRecommendCodeReq implements Serializable {
    private static final long serialVersionUID = -9024185781456390447L;

    /**
     * 微信openId
     */
    private String openId;

    /**
     * 推荐码
     */
    private Long recommendCode;
}