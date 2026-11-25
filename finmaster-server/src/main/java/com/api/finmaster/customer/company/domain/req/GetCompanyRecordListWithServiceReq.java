package com.qykj.finmaster.customer.company.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-06-07
 * @Description
 */
@Data
public class GetCompanyRecordListWithServiceReq extends PageEntity {

    @Serial
    private static final long serialVersionUID = 4503573954355132194L;
    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 管家负责人名称
     */
    private String butlerOwnerUserName;

    /**
     * 开始时间
     */
    private LocalDateTime start;

    /**
     * 结束时间
     */
    private LocalDateTime end;

}