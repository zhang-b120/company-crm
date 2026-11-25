package com.qykj.finmaster.system.productType.domain.req;

import lombok.Data;

import java.io.Serializable;

@Data
public class GetProductTypeReq implements Serializable {
    private static final long serialVersionUID = 1879754796332264068L;
    /**
     * 类型ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 类型名称
     */
    private String name;

    /**
     * 类型编码
     */
    private String code;

    /**
     * 描述
     */
    private String description;
}
