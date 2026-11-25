package com.qykj.finmaster.system.tenant.entity.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-08-01
 * @Description
 */
@Data
public class GetTenantPageReq extends PageEntity {
    @Serial
    private static final long serialVersionUID = 8947758407817578578L;

    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 状态
     */
    private Integer status;
    

}