package com.qykj.finmaster.data.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-29
 * @Description
 */
@Data
public class GetLeadAnalysisDataResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 8129343902934661937L;

    /**
     * 负责人
     */
    private String ownerUser;

    /**
     * 负责人名称
     */
    private String ownerUserName;

    /**
     * 当日线索量
     */
    private Integer todayLeadCount = 0;

    /**
     * 累计线索量
     */
    private Integer allLeadCount = 0;

    /**
     * 当日成交
     */
    private Integer todayDealCount = 0;

    /**
     * 当月成交
     */
    private Integer monthDealCount = 0;

    /**
     * 累计成交
     */
    private Integer allDealCount = 0;

    /**
     * 今日累计注册小程序数
     */
    private Integer todayRegisterApplet = 0;

    /**
     * 累计注册小程序数
     */
    private Integer allRegisterApplet = 0;

    /**
     * 累计注册数/总线索量
     */
    private String allRegisterAllLeadRadio;

    /**
     * 当日成交/当日新增线索数
     */
    private String todayDealTodayLeadRadio;

    /**
     * 当日成交/当日新增注册数
     */
    private String todayDealTodayRegisterRadio;

    /**
     * 累计成交/累计注册数
     */
    private String allDealAllRegisterRadio;

    /**
     * 当日注册数/当日新增线索数
     */
    private String todayRegisterTodayLeadRadio;

    /**
     * 累计成交/总线索数
     */
    private String allDealAllLeadRadio;

}