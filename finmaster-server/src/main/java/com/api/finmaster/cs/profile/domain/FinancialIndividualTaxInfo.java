package com.qykj.finmaster.cs.profile.domain;

import lombok.Data;

/** financial.individual_tax 最新记录 */
@Data
public class FinancialIndividualTaxInfo {
    private Long accountSetsId;
    /** 0=未执行, 1=失败, 2=成功 */
    private Integer result;
    private String failedReason;
}
