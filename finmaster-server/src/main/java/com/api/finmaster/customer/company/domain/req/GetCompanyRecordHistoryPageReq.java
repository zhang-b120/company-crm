package com.qykj.finmaster.customer.company.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-08-25
 * @Description
 */
@Data
public class GetCompanyRecordHistoryPageReq extends PageEntity {
    @Serial
    private static final long serialVersionUID = 1883669209530184692L;

    /**
     * 公司Id
     */
    private Long companyId;
}