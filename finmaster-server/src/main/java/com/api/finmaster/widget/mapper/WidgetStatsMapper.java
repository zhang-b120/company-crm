package com.qykj.finmaster.widget.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 手机桌面小组件统计查询
 */
@Mapper
public interface WidgetStatsMapper {

    /**
     * 账套总数（排除停用）。
     * 暂时跨库直查 financial.fxy_financial_account_sets（与 FinancialProfileMapper 同模式），
     * 财务系统提供统计接口后可切换为 HTTP 调用。
     */
    Long countActiveAccountSets();

    /** 今日创建、业务名称含关键字的订单覆盖的公司数（排除已取消/退款单） */
    Long countTodayCompaniesByBusinessName(@Param("keyword") String keyword, @Param("tenantId") Long tenantId);

    /** 今日创建、含续费明细的订单数（排除已取消/退款单） */
    Long countTodayRenewalOrders(@Param("tenantId") Long tenantId);

    /** 今日新增线索条数（排除已删除） */
    Long countTodayLeads(@Param("tenantId") Long tenantId);
}
