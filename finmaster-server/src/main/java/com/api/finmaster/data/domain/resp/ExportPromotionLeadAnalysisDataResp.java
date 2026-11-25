package com.qykj.finmaster.data.domain.resp;

import com.qykj.finmaster.common.annotation.Excel;
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
public class ExportPromotionLeadAnalysisDataResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 6947204656739412442L;

    /**
     * 负责人名称
     */
    @Excel(title = "负责人", field = "ownerUserName")
    private String ownerUserName;

    /**
     * 有效线索数
     */
    @Excel(title = "有效线索数", field = "validLeadCount")
    private Integer validLeadCount = 0;

    /**
     * 总线索量
     */
    @Excel(title = "总线索量", field = "allLeadCount")
    private Integer allLeadCount = 0;

    /**
     * 销售负责人
     */
    @Excel(title = "销售负责人", field = "dealCount")
    private Integer dealCount = 0;

    /**
     * 成交金额
     */
    @Excel(title = "成交金额", field = "dealAmount")
    private BigDecimal dealAmount;
}