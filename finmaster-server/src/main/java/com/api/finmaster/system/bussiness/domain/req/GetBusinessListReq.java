package com.qykj.finmaster.system.bussiness.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * 业务列表查询请求
 */
@Data
public class GetBusinessListReq extends PageEntity {

    @Serial
    private static final long serialVersionUID = -739284502349875234L;

    /**
     * 业务编码
     */
    private String businessCode;

    /**
     * 业务名称
     */
    private String businessName;
}

