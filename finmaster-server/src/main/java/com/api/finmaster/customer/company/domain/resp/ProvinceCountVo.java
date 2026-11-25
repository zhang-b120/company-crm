package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

/**
 * 大屏：按省份统计的合作中公司数量。
 * province=省份简称（customer_company.province，已由 ProvinceParser 规整）；count=该省合作中公司数。
 */
@Data
public class ProvinceCountVo {

    /** 省份简称（浙江/山东/北京/内蒙古/广西…） */
    private String province;

    /** 该省合作中公司数 */
    private Long count;
}
