package com.qykj.finmaster.history;

import java.time.LocalDate;

/**
 * 公司服务对应的合同日期范围
 * <p>
 * effectiveDate：服务生效日期（最小值）
 * actualEndDate：服务实际截止日期（最大值）
 */
public class HistoryCompanyServiceDateRange {

    private LocalDate effectiveDate;

    private LocalDate actualEndDate;

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public LocalDate getActualEndDate() {
        return actualEndDate;
    }

    public void setActualEndDate(LocalDate actualEndDate) {
        this.actualEndDate = actualEndDate;
    }
}

