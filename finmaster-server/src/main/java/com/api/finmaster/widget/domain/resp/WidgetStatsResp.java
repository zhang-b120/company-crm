package com.qykj.finmaster.widget.domain.resp;

import lombok.Data;

/**
 * 手机桌面小组件统计数据
 */
@Data
public class WidgetStatsResp {

    /** 账套总数（财务系统，排除已停用） */
    private Long accountSetTotal;

    /** 今日代账：今日创建、业务名称含"代账"的订单覆盖的公司数（排除已取消/退款单） */
    private Long todayBookkeeping;

    /** 今日工商：今日创建、业务名称含"工商"的订单覆盖的公司数（排除已取消/退款单） */
    private Long todayBusiness;

    /** 今日续费：今日创建、含续费明细的订单数（排除已取消/退款单） */
    private Long todayRenewal;

    /** 新增线索：今日新增的线索条数（排除已删除） */
    private Long todayLeads;
}
