package com.qykj.finmaster.data.domain.resp;

import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-29
 * @Description
 */
@Data
public class ExportLeadAnalysisDataResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 8129343902934661937L;

    /**
     * 负责人名称
     */
    @Excel(title = "销售负责人", field = "ownerUserName")
    private String ownerUserName;

    /**
     * 当日线索量
     */
    @Excel(title = "当日线索量", field = "todayLeadCount")
    private Integer todayLeadCount = 0;

    /**
     * 累计线索量
     */
    @Excel(title = "累计线索量", field = "allLeadCount")
    private Integer allLeadCount = 0;

    /**
     * 当日成交
     */
    @Excel(title = "当日成交", field = "todayDealCount")
    private Integer todayDealCount = 0;

    /**
     * 当月成交
     */
    @Excel(title = "当月成交", field = "monthDealCount")
    private Integer monthDealCount = 0;

    /**
     * 累计成交
     */
    @Excel(title = "累计成交", field = "allDealCount")
    private Integer allDealCount = 0;

    /**
     * 今日累计注册小程序数
     */
    @Excel(title = "今日累计注册小程序数", field = "todayRegisterApplet")
    private Integer todayRegisterApplet = 0;

    /**
     * 累计注册小程序数
     */
    @Excel(title = "累计注册小程序数", field = "allRegisterApplet")
    private Integer allRegisterApplet = 0;

    /**
     * 累计注册数/总线索量
     */
    @Excel(title = "累计注册数/总线索量", field = "allRegisterAllLeadRadio")
    private String allRegisterAllLeadRadio;

    /**
     * 当日成交/当日新增线索数
     */
    @Excel(title = "当日成交/当日新增线索数", field = "todayDealTodayLeadRadio")
    private String todayDealTodayLeadRadio;

    /**
     * 当日成交/当日新增注册数
     */
    @Excel(title = "当日成交/当日新增注册数", field = "todayDealTodayRegisterRadio")
    private String todayDealTodayRegisterRadio;

    /**
     * 累计成交/累计注册数
     */
    @Excel(title = "累计成交/累计注册数", field = "allDealAllRegisterRadio")
    private String allDealAllRegisterRadio;

    /**
     * 当日注册数/当日新增线索数
     */
    @Excel(title = "当日注册数/当日新增线索数", field = "todayRegisterTodayLeadRadio")
    private String todayRegisterTodayLeadRadio;

    /**
     * 累计成交/总线索数
     */
    @Excel(title = "累计成交/总线索数", field = "allDealAllLeadRadio")
    private String allDealAllLeadRadio;

}