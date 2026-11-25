package com.qykj.finmaster.cs.profile.domain;

import lombok.Data;

/** financial.fxy_financial_account_sets 查询结果 */
@Data
public class FinancialAccountSetInfo {
    private Long id;
    private String creditCode;
    /** SMALL_SCALE=小规模, COMMONLY=一般纳税人 */
    private String vatType;
    private String individualTaxCode;
}
