package com.qykj.finmaster.system.appletUser.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-08-22
 * @Description
 */
@Data
public class GetEffectiveUserInfoResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -2700236692487203981L;

    /**
     * 有效客户数
     */
    private Integer effectiveUserCount = 0;

    /**
     * 奖励金额
     */
    private BigDecimal rewardAmount = BigDecimal.ZERO;
}