package com.qykj.finmaster.widget.service;

import com.qykj.finmaster.widget.domain.resp.WidgetStatsResp;
import com.qykj.finmaster.widget.mapper.WidgetStatsMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * 手机桌面小组件统计服务
 */
@Service
public class WidgetStatsService {

    @Resource
    private WidgetStatsMapper widgetStatsMapper;

    public WidgetStatsResp getStats(Long tenantId) {
        WidgetStatsResp resp = new WidgetStatsResp();
        resp.setAccountSetTotal(widgetStatsMapper.countActiveAccountSets());
        resp.setTodayBookkeeping(widgetStatsMapper.countTodayCompaniesByBusinessName("代账", tenantId));
        resp.setTodayBusiness(widgetStatsMapper.countTodayCompaniesByBusinessName("工商", tenantId));
        resp.setTodayRenewal(widgetStatsMapper.countTodayRenewalOrders(tenantId));
        resp.setTodayLeads(widgetStatsMapper.countTodayLeads(tenantId));
        return resp;
    }
}
