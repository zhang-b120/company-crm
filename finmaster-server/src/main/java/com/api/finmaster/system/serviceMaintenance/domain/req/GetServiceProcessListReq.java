package com.qykj.finmaster.system.serviceMaintenance.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-12
 * @Description
 */
@Data
public class GetServiceProcessListReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 6133303571613389642L;

    private Long id;
    /**
     * 公司Id
     */
    private Long companyId;
}