package com.qykj.finmaster.system.bussiness.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 业务关联产品响应
 */
@Data
public class GetBusinessProductResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 4938572398475234987L;

    private Long id;

    private Long businessId;

    private Long productId;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 产品编号
     */
    private String productNumber;
}

