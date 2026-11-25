package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.util.List;

/**
 * 大屏：合作中公司统计聚合返回。
 * total=全部合作中公司按 DISTINCT tax_number 计数（含无省份的，对齐权威 SQL，保证 KPI 准确）；
 * provinces=有省份的按省去重计数（供地图/榜单）。
 * 「合作中」严格口径：company_status='COOPERATING' 且 存在 company_service(service_id=1) 记账服务。
 */
@Data
public class CompanyScreenStatVo {

    /** 合作中公司总数（DISTINCT tax_number，不分省、不加 deleted，对齐权威 SQL） */
    private Long total;

    /** 按省份分布（去重税号、省份非空、倒序） */
    private List<ProvinceCountVo> provinces;
}
