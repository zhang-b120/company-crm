package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.time.LocalDate;

/**
 * 续费统计明细行：某月（按 company_service.actual_end_date）到期的单条公司服务。
 * 过滤口径与 getRenewalStats 完全一致（公司级 + 服务级排除 + 区间），按服务逐行。
 */
@Data
public class RenewalStatDetailRow {

    /** 公司名称 */
    private String companyName;

    /** 税号 */
    private String taxNumber;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String phone;

    /** 管家负责人 */
    private String butlerOwnerUserName;

    /** 生效日期（起） */
    private LocalDate effectiveDate;

    /** 实际截止日期（止） */
    private LocalDate actualEndDate;
}
