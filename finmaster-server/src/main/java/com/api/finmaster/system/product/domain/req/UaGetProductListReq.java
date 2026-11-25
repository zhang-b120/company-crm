package com.qykj.finmaster.system.product.domain.req;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-19
 * @Description
 */
@Data
public class UaGetProductListReq implements Serializable {
    private static final long serialVersionUID = 2301012927924555434L;

    /**
     * 产品类型
     */
    private String productType;

    /**
     * 公司Id
     */
    private Long companyId;
}
