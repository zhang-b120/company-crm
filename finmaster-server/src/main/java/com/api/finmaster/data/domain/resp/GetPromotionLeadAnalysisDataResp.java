package com.qykj.finmaster.data.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-08-29
 * @Description
 */
@Data
public class GetPromotionLeadAnalysisDataResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 4638179572390292378L;
    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

    /**
     * 有效线索数
     */
    private Integer validLeadCount = 0;

    /**
     * 总线索量
     */
    private Integer allLeadCount = 0;

    /**
     * 成交单数
     */
    private Integer dealCount = 0;

    /**
     * 成交金额
     */
    private BigDecimal dealAmount;
}